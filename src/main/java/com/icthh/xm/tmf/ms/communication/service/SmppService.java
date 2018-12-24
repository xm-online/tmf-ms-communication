package com.icthh.xm.tmf.ms.communication.service;

import static java.nio.charset.Charset.forName;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.jsmpp.bean.Alphabet.ALPHA_DEFAULT;
import static org.jsmpp.bean.Alphabet.ALPHA_UCS2;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_PAYLOAD;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Smpp;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class SmppService {

    private static final byte[] EMPTY_MESSAGE = "".getBytes();

    private final AbsoluteTimeFormatter timeFormatter;
    private final ApplicationProperties appProps;

    public SmppService(ApplicationProperties appProps) {
        this.appProps = appProps;
        this.timeFormatter = new AbsoluteTimeFormatter();
    }

    @SneakyThrows
    private String withSession(Task task)  {
        SMPPSession session = new SMPPSession();
        try {
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
            return task.doWork(session);
        } finally {
            session.unbindAndClose();
        }
    }

    @SneakyThrows
    public String send(String destAdrrs, String message, String senderId) {
         return withSession(session -> {
             Smpp smpp = appProps.getSmpp();

             Alphabet encoding = isAlpha(message) ? ALPHA_DEFAULT : ALPHA_UCS2;

             OctetString payload = toPayload(message);
             OptionalParameter[] parameters = new OptionalParameter[]{payload};

             log.info("Start send messate with text {} and senderId {} to {} in encoding {}", message,
                 senderId, destAdrrs, encoding);

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
                 new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                 (byte) smpp.getReplaceIfPresentFlag(),
                 new GeneralDataCoding(encoding, MessageClass.CLASS1, false),
                 (byte) smpp.getSmDefaultMsgId(),
                 EMPTY_MESSAGE,
                 parameters
             );
             log.info("Message submitted, message_id is {}", messageId);
             return messageId;
         });
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

    public List<String> sendMultipleMessages(List<String> phones, String body, String senderId) {
        List<String> results = new ArrayList<>();
        for (String phone : phones) {
            results.add(send(phone, body, senderId));
        }
        return results;
    }

    @FunctionalInterface
    private interface Task {
       String doWork(SMPPSession session) throws Exception;
    }

}
