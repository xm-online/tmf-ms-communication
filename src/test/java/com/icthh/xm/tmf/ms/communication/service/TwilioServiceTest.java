package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.icthh.xm.tmf.ms.communication.domain.MessageType.Twilio;
import static org.assertj.core.api.Assertions.assertThat;

class TwilioServiceTest {

    TwilioService service;

    @BeforeEach
    public void beforeEach() {
        service = new TwilioService();
        CommunicationSpec.Twilio cfg = new CommunicationSpec.Twilio();
        cfg.setKey("senderKey");
        cfg.setAccountSid("AC528fe950968998cae3d3df2ac4f64fc0");
        cfg.setAuthToken("5c93979e622a013ce781bb9b51eba6b8");
        service.registerSender("test", cfg);
    }

    @Test
    void send() {
        CommunicationMessageCreate m = new CommunicationMessageCreate();
        m.setType(Twilio.name());
        m.setContent("Test message");

        Receiver r = new Receiver();
        r.setPhoneNumber("+380631231212");

        Sender s = new Sender();
        s.setPhoneNumber("+15005550006");
        s.setId("senderKey");

        m.setReceiver(List.of(r));
        m.setSender(s);

        CommunicationMessage test = service.send("test", m);
        assertThat(test.getStatus()).isNotBlank();
        assertThat(test.getHref()).isNotBlank();
    }
}
