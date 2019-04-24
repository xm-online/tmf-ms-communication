package com.icthh.xm.tmf.ms.communication.service;

import static java.nio.charset.StandardCharsets.UTF_16;
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
import org.springframework.stereotype.Component;

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
    private SMPPSession createSession(ApplicationProperties appProps)  {
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
        return session;
    }

    public String send(String destAdrrs, String message, String senderId) throws PDUException, IOException,
                                                                                 InvalidResponseException,
                                                                                 NegativeResponseException,
                                                                                 ResponseTimeoutException {

        Smpp smpp = appProps.getSmpp();

        DataCoding dataCoding = getDataConding(message);
        log.info("Start send messate with text {} and senderId {} to {} in encoding {}", message,
                 senderId, destAdrrs, dataCoding);

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
            destAdrrs,
            new ESMClass(),
            (byte) smpp.getProtocolId(),
            (byte) smpp.getPriorityFlag(),
            timeFormatter.format(new Date()),
            smpp.getValidityPeriod(),
            new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
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
        DataCoding cyrillicConding = createEncoding(ALPHA_UCS2, appProps.getSmpp().getCyrillicEncoding());
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

    private boolean isAlpha(String message) {
        return StandardCharsets.ISO_8859_1.newEncoder().canEncode(message);
    }

    private OctetString toPayload(String message) throws UnsupportedEncodingException {
       if (isAlpha(message)) {
           return new OctetString(MESSAGE_PAYLOAD.code(), message);
       } else {
           return new OctetString(MESSAGE_PAYLOAD.code(), message, UTF_16.name());
       }
    }

    private String getSourceAddr(String senderId, Smpp smpp) {
        return isBlank(senderId) ? smpp.getSourceAddr() : senderId;
    }

    @SneakyThrows
    public List<String> sendMultipleMessages(List<String> phones, String body, String senderId) {
        List<String> results = new ArrayList<>();
        for (String phone : phones) {
            results.add(send(phone, body, senderId));
        }
        return results;
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        session.unbindAndClose();
    }
}
