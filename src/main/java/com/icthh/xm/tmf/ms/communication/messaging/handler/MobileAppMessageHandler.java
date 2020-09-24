package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@LepService
@Slf4j
public class MobileAppMessageHandler implements BasicMessageHandler {

    private final FirebaseService firebaseService;
    private final CommunicationMessageMapper mapper;

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        log.debug("Handling message {}", message);
        return firebaseService.sendPushNotification(message);
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        log.debug("Handling message {}", messageCreate);
        return firebaseService.sendPushNotification(mapper.messageCreateToMessage(messageCreate));
    }
}
