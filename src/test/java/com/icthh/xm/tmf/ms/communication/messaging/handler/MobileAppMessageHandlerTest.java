package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandler.DELIVERY_REPORT;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.message;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;

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
//        verify(firebaseService).sendPushNotification(refEq(message.getReceiver()), refEq(message.getCharacteristic())); //todo V!!:
    }

    @Test
    public void handleMessageCreateTest() {
        CommunicationMessageCreate message = messageCreate();
        mobileAppMessageHandler.handle(message);
//        verify(firebaseService).sendPushNotification(refEq(message.getReceiver()), refEq(message.getCharacteristic())); //todo V!!:
    }

    public static CommunicationMessageCreate messageCreate() {
        CommunicationMessageCreate message = new CommunicationMessageCreate();
        Receiver receiver = new Receiver();
        receiver.setPhoneNumber("PH");
        receiver.setId("ID");
        message.setCharacteristic(Lists.newArrayList(new CommunicationRequestCharacteristic() {
            {
                name(DELIVERY_REPORT);
                value("1");
            }
        }));
        return message;
    }

}
