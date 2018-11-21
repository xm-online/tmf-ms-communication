package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
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

import static org.jsmpp.bean.NumberingPlanIndicator.UNKNOWN;
import static org.jsmpp.bean.TypeOfNumber.INTERNATIONAL;

@Slf4j
@Component
public class SmppService {

    private final SmppConfiguration smppConfiguration;
    private final AbsoluteTimeFormatter timeFormatter;
    private final ApplicationProperties appProps;

    public SmppService(SmppConfiguration smppConfiguration, ApplicationProperties appProps) {
        this.smppConfiguration = smppConfiguration;
        this.appProps = appProps;
        this.timeFormatter = new AbsoluteTimeFormatter();
    }

    public void send(String destAdrrs, String message) {
        SMPPSession session = null;
        try {
            session = this.smppConfiguration.getSession();
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
        } catch (PDUException e) {
            log.error("Invalid PDU parameter {}", e);
            throw new IllegalStateException("Invalid PDU parameter");
        } catch (ResponseTimeoutException e) {
            log.error("Response timeout {}", e);
            throw new IllegalStateException("Response timeout");
        } catch (InvalidResponseException e) {
            log.error("Received invalid response {}", e);
            throw new IllegalStateException("Received invalid response");
        } catch (NegativeResponseException e) {
            log.error("Receive negative response {}", e);
            throw new IllegalStateException("Receive negative response");
        } catch (IOException e) {
            log.error("IO error occur {}", e);
            throw new IllegalStateException("IO error occur");
        } finally {
            if (session != null) {
                session.unbindAndClose();
            }
        }
    }

}
