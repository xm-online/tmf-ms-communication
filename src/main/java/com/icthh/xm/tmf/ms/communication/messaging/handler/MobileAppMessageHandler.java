package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@LepService
public class MobileAppMessageHandler implements BasicMessageHandler {

    private final FirebaseService firebaseService;
    private final CommunicationMessageMapper mapper;

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        return firebaseService.sendPushNotification(message);
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessageCreate handle(CommunicationMessageCreate messageCreate) {
        firebaseService.sendPushNotification(mapper.messageCreateToMessage(messageCreate));
        return messageCreate; //todo V: think it over, perhaps re-map the response is correct way here
    }
}
