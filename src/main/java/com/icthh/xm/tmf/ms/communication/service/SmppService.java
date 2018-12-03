package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class SmppService {

    public static final int CONNECT_TIMEOUT = 10000;
    private final AbsoluteTimeFormatter timeFormatter;
    private final ApplicationProperties appProps;

    public SmppService(ApplicationProperties appProps) {
        this.appProps = appProps;
        this.timeFormatter = new AbsoluteTimeFormatter();
    }

    public SMPPSession getSession()  {
        SMPPSession session = new SMPPSession();
        try {
            ApplicationProperties.Smpp smpp = appProps.getSmpp();
            BindParameter bindParam = new BindParameter(
                smpp.getBindType(),
                smpp.getSystemId(),
                smpp.getPassword(),
                smpp.getSystemType(),
                smpp.getAddrTon(),
                smpp.getAddrNpi(),
                smpp.getAddressRange()
            );
            session.setTransactionTimer(CONNECT_TIMEOUT);
            session.connectAndBind(smpp.getHost(), smpp.getPort(), bindParam);
            return session;
        } catch (IOException e) {
            throw new IllegalStateException("Can't connect to smsc server", e);
        }
    }

    public String send(SMPPSession session, String destAdrrs, String message) {
        try {
            ApplicationProperties.Smpp smpp = appProps.getSmpp();
            String messageId = session.submitShortMessage(
                smpp.getServiceType(),
                smpp.getSourceAddrTon(),
                smpp.getSourceAddrNpi(),
                smpp.getSourceAddr(),
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
        } catch (Exception e) {
            log.error("Exception during sending sms", e);
            throw new IllegalStateException("Exception during sending sms", e);
        }
    }

    public List<String> sendMultipleMessages(List<String> phones, String body) {
        SMPPSession session = null;
        List<String> results = new ArrayList<>();
        try {
            session = getSession();
            for (String phone : phones) {
                results.add(send(session, phone, body));
            }
        } finally {
            if (session != null) {
                session.unbindAndClose();
            }
        }
        return results;
    }

}
