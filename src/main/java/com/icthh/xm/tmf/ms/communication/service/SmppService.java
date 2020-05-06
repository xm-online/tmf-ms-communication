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

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.Session;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmppService {

    private static final byte[] EMPTY_MESSAGE = "".getBytes();

    private final AbsoluteTimeFormatter timeFormatter = new AbsoluteTimeFormatter();
    private final ApplicationProperties appProps;
    private final List<DeliveryReportListener> deliveryReportListeners;

    private volatile SMPPSession session;

    @SneakyThrows
    @PostConstruct
    private void createSession() {
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
        this.session = session;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (appProps.getSmpp().getEnabled()) {
            getActualSession();
        }
    }

    public String send(String destAddrs, String message, String senderId, byte deliveryReport, String validityPeriod) throws PDUException, IOException,
        InvalidResponseException,
        NegativeResponseException,
        ResponseTimeoutException {

        Smpp smpp = appProps.getSmpp();

        DataCoding dataCoding = getDataConding(message);
        log.info("Start send message from: {} to: {} with encoding [{}] and content.size: {}", senderId, destAddrs,
            dataCoding, message.length());

        OctetString payload = toPayload(message);
        OptionalParameter[] parameters = new OptionalParameter[]{payload};

        SMPPSession session = getActualSession();

        String messageId = session.submitShortMessage(
            smpp.getServiceType(),
            smpp.getSourceAddrTon(),
            smpp.getSourceAddrNpi(),
            getSourceAddr(senderId, smpp),
            smpp.getDestAddrTon(),
            smpp.getDestAddrNpi(),
            destAddrs,
            new ESMClass(),
            (byte) smpp.getProtocolId(),
            (byte) smpp.getPriorityFlag(),
            timeFormatter.format(new Date()),
            validityPeriod,
            new RegisteredDelivery(deliveryReport),
            (byte) smpp.getReplaceIfPresentFlag(), dataCoding,
            (byte) smpp.getSmDefaultMsgId(),
            EMPTY_MESSAGE,
            parameters
        );
        log.info("Message submitted, message_id is {}", messageId);
        return messageId;

    }

    private DataCoding getDataConding(String message) {
        DataCoding alphaConding = createEncoding(ALPHA_DEFAULT, appProps.getSmpp().getAlphaEncoding());
        DataCoding cyrillicConding = createEncoding(ALPHA_UCS2, appProps.getSmpp().getNotAlphaEncoding());
        return isAlpha(message) ? alphaConding : cyrillicConding;
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
            if (!session.getSessionState().isBound()) {
                session.unbindAndClose();
                createSession();
                session = this.session;
            }
        }
        return session;
    }

    private boolean isAlpha(String message) {
        return StandardCharsets.ISO_8859_1.newEncoder().canEncode(message);
    }

    private OctetString toPayload(String message) throws UnsupportedEncodingException {
        if (isAlpha(message)) {
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
