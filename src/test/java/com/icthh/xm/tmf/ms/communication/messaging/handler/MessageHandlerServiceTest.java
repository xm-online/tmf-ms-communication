package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageHandlerServiceTest {

    @Mock
    private SmppMessagingHandler smppMessagingHandler;
    @Mock
    private CustomCommunicationMessageHandler customCommunicationMessageHandler;
    @Mock
    private MobileAppMessageHandler mobileAppMessageHandler;
    @Mock
    private TwilioMessageHandler twilioMessageHandler;

    @InjectMocks
    MessageHandlerService messageHandlerService;

    @Test(expected = IllegalArgumentException.class)
    public void nullMessageTyeTest() {
        messageHandlerService.getHandler(null);
    }

    @Test
    public void getHandlerTest() {
        messageHandlerService.init();
        assertEquals(messageHandlerService.getHandler(MessageType.MobileApp.name()), mobileAppMessageHandler);
        assertEquals(messageHandlerService.getHandler(MessageType.SMS.name()), smppMessagingHandler);
        assertEquals(messageHandlerService.getHandler(MessageType.Viber.name()), customCommunicationMessageHandler);
        assertEquals(messageHandlerService.getHandler(MessageType.Telegram.name()), customCommunicationMessageHandler);
        assertEquals(messageHandlerService.getHandler(MessageType.Twilio.name()), twilioMessageHandler);
    }
}
