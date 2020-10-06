package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service.ViberService;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    SmppMessagingHandler.class,
    CustomCommunicationMessageHandler.class,
    MobileAppMessageHandler.class,
    ViberMessageHandler.class,
    MessageHandlerService.class,
})
@MockBeans({
    @MockBean(classes = {FirebaseService.class}),
    @MockBean(classes = {ViberService.class}),
    @MockBean(classes = {KafkaTemplate.class}),
    @MockBean(classes = {SmppService.class}),
    @MockBean(classes = {ApplicationProperties.class}),
    @MockBean(classes = {BusinessRuleValidator.class})
})
@TestPropertySource(properties="application.smpp.enabled=true")
public class MessageHandlerServiceTest {


    @Autowired
    private SmppMessagingHandler smppMessagingHandler;
    @Autowired
    private CustomCommunicationMessageHandler customCommunicationMessageHandler;
    @Autowired
    private MobileAppMessageHandler mobileAppMessageHandler;
    @Autowired
    private ViberMessageHandler viberMessageHandler;

    @Autowired
    MessageHandlerService messageHandlerService;

    @Test(expected = IllegalArgumentException.class)
    public void nullMessageTyeTest() {
        messageHandlerService.getHandler(null);
    }

    @Test
    public void getHandlerTest() {
        assertEquals(messageHandlerService.getHandler(MessageType.MobileApp.name()), mobileAppMessageHandler);
        assertEquals(messageHandlerService.getHandler(MessageType.SMS.name()), smppMessagingHandler);
        assertEquals(messageHandlerService.getHandler(MessageType.Viber.name()), viberMessageHandler);
        assertEquals(messageHandlerService.getHandler(MessageType.Telegram.name()), customCommunicationMessageHandler);
    }
}
