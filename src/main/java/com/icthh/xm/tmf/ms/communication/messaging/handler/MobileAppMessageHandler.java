package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class MobileAppMessageHandler implements BasicMessageHandler {

    private final FirebaseService firebaseService;
    private final CommunicationMessageMapper mapper;

    @Override
    public void handle(CommunicationMessage message) { //todo V!: add LEP here
        firebaseService.sendPushNotification(message);
    }

    @Override
    public void handle(CommunicationMessageCreate messageCreate) { //todo V!: add LEP here
        firebaseService.sendPushNotification(mapper.messageCreateToMessage(messageCreate));
    }
}
