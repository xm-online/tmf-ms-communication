package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static org.junit.Assert.assertEquals;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

    MessageHandlerService messageHandlerService;

    @Before
    public void setup() {
        messageHandlerService = new MessageHandlerService(smppMessagingHandler,
            customCommunicationMessageHandler, twilioMessageHandler,
            Optional.of(mobileAppMessageHandler));
    }

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
