package com.icthh.xm.tmf.ms.communication.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
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
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.bean.OptionalParameter.Tag;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
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
             log.info("Start send messate with text {} and senderId {} to {}", message, senderId, destAdrrs);

             OptionalParameter[] parameters = new OptionalParameter[]{new OptionalParameter.OctetString(MESSAGE_PAYLOAD.code(), message)};

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
                 new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false),
                 (byte) smpp.getSmDefaultMsgId(),
                 message.getBytes()
             );
             log.info("Message submitted, message_id is {}", messageId);
             return messageId;
         });
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



//    private void submitSm() {
//        OptionalParameter[] parameters;
//        parameters = (OptionalParameter[]) ArrayUtils
//            .add(optionalParameters, new OptionalParameter.OctetString(Tag.MESSAGE_PAYLOAD.code(),
//                message));
//        return submitSm(EMPTY_MESSAGE,
//            dataCoding,
//            destAddress,
//            sendDeliveryReport,
//            transformResult,
//            validityPeriod,
//            parameters);
//    }
//
//    public String submitSm(
//        final byte[] message,
//        final DataCoding dataCoding,
//        final String destAddress,
//        final Boolean sendDeliveryReport,
//        final TransformResult transformResult,
//        final String validityPeriod,
//        final OptionalParameter... optionalParameters) throws SmppChannelException {
//
//        try {
//            ensureConnectionExists();
//            return this.getSmppConnection().submitShortMessage(transformResult.getSourceAddress(), destAddress, dataCoding,
//                message, sendDeliveryReport, transformResult.getTon(), transformResult.getNpi(), validityPeriod, optionalParameters);
//
//        } catch (Exception e) {
//            //LOGGER.error("Failed to sumbitSm.", e);
//            throw new SmppChannelException(String.format("because error: %s",
//                e.getMessage()), e);
//
//        }
//    }

    @FunctionalInterface
    private interface Task {
       String doWork(SMPPSession session) throws Exception;
    }

}
