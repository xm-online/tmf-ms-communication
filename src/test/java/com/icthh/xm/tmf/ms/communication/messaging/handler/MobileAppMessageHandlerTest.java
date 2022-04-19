package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.message;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import com.icthh.xm.tmf.ms.communication.messaging.handler.logic.FirebaseMessageHelper;
import com.icthh.xm.tmf.ms.communication.service.firebase.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class MobileAppMessageHandlerTest {

    @Mock
    FirebaseService firebaseService;

    @InjectMocks
    MobileAppMessageHandler mobileAppMessageHandler;

    @InjectMocks
    FirebaseMessageHelper mobileAppMessageHandlerSteps;

    @Test
    public void handleMessageTest() {
        CommunicationMessage message = message();
        mobileAppMessageHandler.handle(message);
        verify(firebaseService).sendPushNotification(message);
    }

    @Test
    public void splitMessagesByCharacteristicsTest() {
        CommunicationMessageCreate srcMessage = getTestCharacteristics();
        List<CommunicationMessageCreate> splittedMessage = mobileAppMessageHandlerSteps.splitMessagesByCharacteristics(srcMessage);
        assertEquals(4, splittedMessage.size());
        for (CommunicationMessageCreate testedItem : splittedMessage) {
            List receiverIds = testedItem.getReceiver().stream().map(recr -> recr.getName()).collect(Collectors.toList());
            if (receiverIds.contains("5")) {
                assert receiverIds.contains("6");
                assertEquals(testedItem.getCharacteristic().size(), 2);
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("A").value("AA"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("B").value("BB"));
            } else if (receiverIds.contains("3")) {
                assertEquals(testedItem.getCharacteristic().size(), 4);
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("A").value("AA"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("B").value("BB"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("D").value("DD"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("E").value("EE"));
            } else if (receiverIds.contains("4")) {
                assertEquals(testedItem.getCharacteristic().size(), 4);
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("A").value("AA"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("B").value("BB"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("D").value("DD"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("E").value("EEE"));
            } else if (receiverIds.contains("1")) {
                assert receiverIds.contains("2");
                assertEquals(testedItem.getCharacteristic().size(), 3);
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("A").value("changedAA"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("B").value("BB"));
                assert testedItem.getCharacteristic().contains(new CommunicationRequestCharacteristic().name("C").value("CC"));
            }
        }
    }

    private CommunicationMessageCreate getTestCharacteristics() {
        List<CommunicationRequestCharacteristic> commonCharacteristics = new ArrayList();
        commonCharacteristics.add(new CommunicationRequestCharacteristic().name("A").value("AA"));
        commonCharacteristics.add(new CommunicationRequestCharacteristic().name("B").value("BB"));

        List<Receiver> receivers = new ArrayList();
        List<CommunicationRequestCharacteristic> r1Characteristics = new ArrayList();
        r1Characteristics.add(new CommunicationRequestCharacteristic().name("A").value("changedAA"));
        r1Characteristics.add(new CommunicationRequestCharacteristic().name("C").value("CC"));
        receivers.add(new Receiver().name("1").characteristic(r1Characteristics));

        List<CommunicationRequestCharacteristic> r2Characteristics = new ArrayList();
        r2Characteristics.add(new CommunicationRequestCharacteristic().name("A").value("changedAA"));
        r2Characteristics.add(new CommunicationRequestCharacteristic().name("C").value("CC"));
        receivers.add(new Receiver().name("2").characteristic(r2Characteristics));

        List<CommunicationRequestCharacteristic> r3Characteristics = new ArrayList();
        r3Characteristics.add(new CommunicationRequestCharacteristic().name("D").value("DD"));
        r3Characteristics.add(new CommunicationRequestCharacteristic().name("E").value("EE"));
        receivers.add(new Receiver().name("3").characteristic(r3Characteristics));

        List<CommunicationRequestCharacteristic> r4Characteristics = new ArrayList();
        r4Characteristics.add(new CommunicationRequestCharacteristic().name("D").value("DD"));
        r4Characteristics.add(new CommunicationRequestCharacteristic().name("E").value("EEE"));
        receivers.add(new Receiver().name("4").characteristic(r4Characteristics));

        receivers.add(new Receiver().name("5"));
        receivers.add(new Receiver().name("6"));

        CommunicationMessageCreate msg = new CommunicationMessageCreate()
                .characteristic(commonCharacteristics)
                .receiver(receivers)
                .atType("MobileApp")
                .status("Ok");

        return msg;
    }
}
