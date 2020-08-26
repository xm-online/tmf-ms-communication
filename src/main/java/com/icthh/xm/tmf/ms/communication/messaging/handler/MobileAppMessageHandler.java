package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class MobileAppMessageHandler implements BasicMessageHandler {

    private final FirebaseService firebaseService;

    @Override
    public void handle(CommunicationMessage message) {
        firebaseService.sendPushNotification(message.getReceiver(), message.getCharacteristic());
    }

    @Override
    public void handle(CommunicationMessageCreate messageCreate) {
        firebaseService.sendPushNotification(messageCreate.getReceiver(), messageCreate.getCharacteristic());
    }

    @Override
    public MessageType getType() {
        return MessageType.MobileApp;
    }
}
