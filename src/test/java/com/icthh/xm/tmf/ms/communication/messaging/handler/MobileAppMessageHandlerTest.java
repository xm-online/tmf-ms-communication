package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.message;
import static org.mockito.Mockito.verify;

import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MobileAppMessageHandlerTest {

    @Mock
    FirebaseService firebaseService;

    @InjectMocks
    MobileAppMessageHandler mobileAppMessageHandler;

    @Test
    public void handleMessageTest() {
        CommunicationMessage message = message();
        mobileAppMessageHandler.handle(message);
        verify(firebaseService).sendPushNotification(message);
    }
}
