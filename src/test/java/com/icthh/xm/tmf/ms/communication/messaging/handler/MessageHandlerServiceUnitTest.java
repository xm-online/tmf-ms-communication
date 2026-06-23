package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MessageHandlerServiceUnitTest {

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

    @BeforeEach
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

        lenient().when(smppMessagingHandler.getType()).thenReturn(MessageType.SMS);
        lenient().when(customCommunicationMessageHandler.getType()).thenReturn(MessageType.Custom);
        lenient().when(twilioMessageHandler.getType()).thenReturn(MessageType.Twilio);
        lenient().when(mobileAppMessageHandler.getType()).thenReturn(MessageType.MobileApp);
        lenient().when(emailMessageHandler.getType()).thenReturn(MessageType.Email);
    }

    @Test
    public void nullMessageTyeTest() {
        assertThrows(IllegalArgumentException.class, () -> messageHandlerService.getHandler(null));
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
        assertEquals(messageHandlerService.getHandler("random-non-blank-message-type"), customCommunicationMessageHandler);
    }
}
