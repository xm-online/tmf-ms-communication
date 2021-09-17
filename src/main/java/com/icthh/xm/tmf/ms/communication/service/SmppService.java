package com.icthh.xm.tmf.ms.communication.service;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.jsmpp.bean.Alphabet.ALPHA_DEFAULT;
import static org.jsmpp.bean.Alphabet.ALPHA_UCS2;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_PAYLOAD;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Smpp;
import com.icthh.xm.tmf.ms.communication.messaging.MessageReceiverListenerAdapter;
import com.icthh.xm.tmf.ms.communication.service.smpp.ValidityPeriodFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.StringParameter;
import org.jsmpp.util.StringValidator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmppService {

    private final AbsoluteTimeFormatter timeFormatter = new AbsoluteTimeFormatter();
    private final ApplicationProperties appProps;
    private final List<DeliveryReportListener> deliveryReportListeners;
    private final ValidityPeriodFactory validityPeriodFactory;

    private volatile SMPPSession session;

    @SneakyThrows
    private SMPPSession createSession(ApplicationProperties appProps) {
        SMPPSession session = new SMPPSession();
        Smpp smpp = appProps.getSmpp();
        BindParameter bindParam = new BindParameter(
                smpp.getBindType(),
                smpp.getSystemId(),
                smpp.getPassword(),
                smpp.getSystemType(),
                smpp.getAddrTon(),
                smpp.getAddrNpi(),
                smpp.getAddressRange()
        );
        session.setTransactionTimer(smpp.getConnectionTimeout());
        session.connectAndBind(smpp.getHost(), smpp.getPort(), bindParam);
        session.setMessageReceiverListener((MessageReceiverListenerAdapter) (deliverySm) -> {
            try {
                MdcUtils.putRid(MdcUtils.generateRid());
                deliveryReportListeners.forEach(it -> it.onAcceptDeliverSm(deliverySm));
            } catch (Exception e) {
                log.error("Error process delivery report", e);
            } finally {
                MdcUtils.removeRid();
            }
        });
        session.addSessionStateListener((newState, oldState, source) -> {
            if (!newState.isBound()) {
                getActualSession();
            }
        });
        return session;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (appProps.getSmpp().getEnabled()) {
            getActualSession();
        }
    }

    /**
     * Send a message
     *
     * @param destAddrs          destination number
     * @param message            text message
     * @param senderId           sender number or alpha-name
     * @param optionalParameters optional parameters, key - parameter tag, value - parameter value
     * @param validityPeriod     a number of seconds a message is valid.
     *                           If is not delivered in this period will get EXPIRED delivery state
     *                           (uses the default if null).
     * @param protocolId         smpp protocol id (uses the default if null)
     * @return message id - unique identifier of the message. Used in delivery reports.
     */
    public String send(String destAddrs, String message, String senderId, byte deliveryReport, Map<Short,
            String> optionalParameters, Integer validityPeriod, Integer protocolId) throws PDUException, IOException,
            InvalidResponseException,
            NegativeResponseException,
            ResponseTimeoutException {

        Smpp smpp = appProps.getSmpp();

        DataCoding dataCoding = getDataCoding(message);
        log.info("Start send message from: {} to: {} with encoding [{}] and content.size: {}, " +
                        "optional parameters: {}, validity period: {}, protocol id: {}", senderId, destAddrs,
                dataCoding, message.length(), optionalParameters, validityPeriod, protocolId);

        List<OctetString> optional = optionalParameters.entrySet().stream()
                .map(e -> new OctetString(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        String shortMessage = "";
        if (isValidShortMessage(message)) {
            shortMessage = message;
        } else {
            optional.add(toPayload(message));
        }

        SMPPSession session = getActualSession();

        Date scheduleDeliveryTime = new Date();
        String messageId = session.submitShortMessage(
                smpp.getServiceType(),
                smpp.getSourceAddrTon(),
                smpp.getSourceAddrNpi(),
                getSourceAddr(senderId, smpp),
                smpp.getDestAddrTon(),
                smpp.getDestAddrNpi(),
                destAddrs,
                new ESMClass(),
                (protocolId != null ? protocolId.byteValue() : (byte) smpp.getProtocolId()),
                (byte) smpp.getPriorityFlag(),
                timeFormatter.format(scheduleDeliveryTime),
                validityPeriodFactory.asString(validityPeriod, scheduleDeliveryTime),
                new RegisteredDelivery(deliveryReport),
                (byte) smpp.getReplaceIfPresentFlag(), dataCoding,
                (byte) smpp.getSmDefaultMsgId(),
                shortMessage.getBytes(),
                optional.toArray(OctetString[]::new)
        );
        log.info("Message submitted, message_id is {}", messageId);
        return messageId;
    }

    private boolean isValidShortMessage(String message) {
        try {
            StringValidator.validateString(message.getBytes(), StringParameter.SHORT_MESSAGE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private DataCoding getDataCoding(String message) {
        return isLatin(message)
                ? createEncoding(ALPHA_DEFAULT, appProps.getSmpp().getAlphaEncoding())
                : createEncoding(ALPHA_UCS2, appProps.getSmpp().getNotAlphaEncoding());
    }

    private DataCoding createEncoding(Alphabet defaultEncoding, Byte encoding) {
        return encoding == null ? new GeneralDataCoding(defaultEncoding, MessageClass.CLASS1, false) : () -> encoding;
    }

    private SMPPSession getActualSession() {
        SMPPSession session = this.session;
        if (session != null && session.getSessionState().isBound()) {
            return session;
        }

        synchronized (this) {
            session = this.session;
            if (session == null) {
                session = createSession(appProps);
                this.session = session;
            } else if (!session.getSessionState().isBound()) {
                session.unbindAndClose();
                session = createSession(appProps);
                this.session = session;
            }
        }
        return session;
    }

    private boolean isLatin(String message) {
        return StandardCharsets.ISO_8859_1.newEncoder().canEncode(message);
    }

    private OctetString toPayload(String message) throws UnsupportedEncodingException {
        if (isLatin(message)) {
            return new OctetString(MESSAGE_PAYLOAD.code(), message);
        } else {
            return new OctetString(MESSAGE_PAYLOAD.code(), message, UTF_16BE.name());
        }
    }

    private String getSourceAddr(String senderId, Smpp smpp) {
        return isBlank(senderId) ? smpp.getSourceAddr() : senderId;
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        session.unbindAndClose();
    }
}
