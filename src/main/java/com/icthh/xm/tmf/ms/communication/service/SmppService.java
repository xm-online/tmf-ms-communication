package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.jsmpp.bean.NumberingPlanIndicator.UNKNOWN;
import static org.jsmpp.bean.TypeOfNumber.INTERNATIONAL;

@Slf4j
@Component
public class SmppService {

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
            session.connectAndBind(smpp.getHost(), smpp.getPort(), bindParam);
        } catch (IOException e) {
            throw new IllegalStateException("Can't connect to smsc server", e);
        }
        return session;
    }

    public void send(SMPPSession session, String destAdrrs, String message) {
        try {
            String messageId = session.submitShortMessage(
                appProps.getSmpp().getServiceType(),
                INTERNATIONAL,
                UNKNOWN,
                appProps.getSmpp().getSourceAddr(),
                INTERNATIONAL,
                UNKNOWN,
                destAdrrs,
                new ESMClass(),
                (byte) 0,
                (byte) 1,
                timeFormatter.format(new Date()),
                null,
                new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                (byte) 0,
                new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false),
                (byte) 0,
                message.getBytes()
            );
            log.info("Message submitted, message_id is {}", messageId);
        } catch (Exception e) {
            log.error("Exception during sending sms", e);
            throw new IllegalStateException("Exception during sending sms", e);
        }
    }

    public void sendMultipleMessages(List<String> phones, String body) {
        SMPPSession session = null;
        try {
            session = getSession();
            for (String phone : phones) {
                send(session, phone, body);
            }
        } finally {
            if (session != null) {
                session.unbindAndClose();
            }
        }
    }

}
