package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@LepService(group = "service.message")
@Service
@Slf4j
@AllArgsConstructor
public class CustomCommunicationMessageHandler implements BasicMessageHandler {

    private final CommunicationMessageMapper communicationMessageMapper;

    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        warnIfNoLogic(message.getType());
        return message;
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        warnIfNoLogic(messageCreate.getType());
        return communicationMessageMapper.messageCreateToMessage(messageCreate);
    }

    @Override
    public MessageType getType() {
        return MessageType.Custom;
    }

    private void warnIfNoLogic(String type) {
        log.warn("There is no logic for the message type: {}", type);
    }
}
