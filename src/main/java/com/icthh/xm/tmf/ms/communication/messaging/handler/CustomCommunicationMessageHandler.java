package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@LepService(group = "service.message")
@Service
@Slf4j
public class CustomCommunicationMessageHandler implements BasicMessageHandler{

    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public void handle(CommunicationMessage message){
        warnIfNoLogic(message.getType());
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public void handle(CommunicationMessageCreate message) {
        warnIfNoLogic(message.getType());
    }

    private void warnIfNoLogic(String type) {
        log.warn("There is no logic for the message type: {}", type);
    }
}
