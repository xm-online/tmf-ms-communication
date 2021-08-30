package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static java.util.Optional.ofNullable;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.EnumMap;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LepService(group = "service")
@RequiredArgsConstructor
public class MessageHandlerService {

    private final List<BasicMessageHandler> messageHandlerList;
    private final EnumMap<MessageType, BasicMessageHandler> messageHandlerMap = new EnumMap<>(MessageType.class);

    @PostConstruct
    void init() {
        messageHandlerList.forEach(handler -> {
            messageHandlerMap.put(handler.getType(), handler);
        });
    }

    public BasicMessageHandler getHandler(String typeString) {
        MessageType type = ofNullable(typeString)
            .map(MessageType::valueOf)
            .orElseThrow(() -> new IllegalArgumentException("Message type must exist"));

        BasicMessageHandler messageHandler = messageHandlerMap.get(type);
        if (messageHandler == null) {
            log.warn("getHandler: no handler defined for type: {}, going to return default handler", typeString);
            messageHandler = ofNullable(messageHandlerMap.get(MessageType.Custom))
                .orElseThrow(() -> new IllegalArgumentException("Could not resolve handler for type: " + typeString));
        }

        return messageHandler;
    }

    @LogicExtensionPoint(value = "RetrieveCommunicationMessage")
    public List<CommunicationMessage> retrieveCommunicationMessage(String id) {
        return List.of();
    }

}
