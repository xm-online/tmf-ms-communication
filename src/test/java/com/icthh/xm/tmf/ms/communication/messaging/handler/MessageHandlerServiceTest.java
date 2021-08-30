package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import java.util.List;
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
    @Mock
    private EmailMessageHandler emailMessageHandler;

    MessageHandlerService messageHandlerService;

    @Before
    public void setup() {
        messageHandlerService = new MessageHandlerService(
            List.of(
                smppMessagingHandler,
                customCommunicationMessageHandler,
                twilioMessageHandler,
                mobileAppMessageHandler,
                emailMessageHandler
            )
        );

        when(smppMessagingHandler.getType()).thenReturn(MessageType.SMS);
        when(customCommunicationMessageHandler.getType()).thenReturn(MessageType.Custom);
        when(twilioMessageHandler.getType()).thenReturn(MessageType.Twilio);
        when(mobileAppMessageHandler.getType()).thenReturn(MessageType.MobileApp);
        when(emailMessageHandler.getType()).thenReturn(MessageType.Email);
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
        assertEquals(messageHandlerService.getHandler(MessageType.Email.name()), emailMessageHandler);
    }
}
