package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.SmppConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

@Slf4j
@Component
public class SmppService {


    private final SmppConfiguration smppConfiguration;
    private final AbsoluteTimeFormatter timeFormatter;

    public SmppService(SmppConfiguration smppConfiguration) {
        this.smppConfiguration = smppConfiguration;
        this.timeFormatter = new AbsoluteTimeFormatter();
    }


    public void send(String destAdrrs, String message) {
        SMPPSession session = null;
        try {
            session = this.smppConfiguration.getSession();
            String messageId = session.submitShortMessage(
                "CMT",
                TypeOfNumber.INTERNATIONAL,
                NumberingPlanIndicator.UNKNOWN,
                "1616",
                TypeOfNumber.INTERNATIONAL,
                NumberingPlanIndicator.UNKNOWN,
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
        } catch (PDUException e) {
            log.error("Invalid PDU parameter {}", e);
        } catch (ResponseTimeoutException e) {
            log.error("Response timeout {}", e);
        } catch (InvalidResponseException e) {
            log.error("Received invalid response {}", e);
        } catch (NegativeResponseException e) {
            log.error("Receive negative response {}", e);
        } catch (IOException e) {
            log.error("IO error occur {}", e);
        }
        if (session != null) {
            session.unbindAndClose();
        }
    }

}
