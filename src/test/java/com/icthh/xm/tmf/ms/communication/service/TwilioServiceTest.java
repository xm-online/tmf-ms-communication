package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static com.icthh.xm.tmf.ms.communication.domain.MessageType.Twilio;
import static org.assertj.core.api.Assertions.assertThat;

class TwilioServiceTest {

    TwilioService service;

    KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    public void beforeEach() {
        kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        ApplicationProperties ap = new ApplicationProperties();
        ApplicationProperties.Messaging messaging = new ApplicationProperties.Messaging();
        messaging.setReciveQueueNameTemplate("tmpl_%s_%s_receive");
        ap.setMessaging(messaging);
        service = new TwilioService(new ObjectMapper(), ap, kafkaTemplate,
            Mappers.getMapper(CommunicationMessageMapper.class));
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
