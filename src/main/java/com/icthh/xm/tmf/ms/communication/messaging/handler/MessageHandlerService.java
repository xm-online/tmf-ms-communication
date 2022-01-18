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
import org.apache.commons.lang3.StringUtils;

@Slf4j
@LepService(group = "service")
@RequiredArgsConstructor
public class MessageHandlerService {

    private final List<BasicMessageHandler> messageHandlerList;
    private final EnumMap<MessageType, BasicMessageHandler> messageHandlerMap = new EnumMap<>(MessageType.class);

    @PostConstruct
    void init() {
        messageHandlerList.forEach(handler -> {
            log.info("init: processing handler for type: {}", handler.getType());
            messageHandlerMap.put(handler.getType(), handler);
        });
    }

    public BasicMessageHandler getHandler(String typeString) {
        if (StringUtils.isBlank(typeString)) {
            throw new IllegalArgumentException("Message type must exist");
        }
        MessageType type;
        try {
            type = MessageType.valueOf(typeString);
        } catch (IllegalArgumentException ignore) {
            log.info("getHandler: no message type enum defined for type: {}, proceeding with: {}",
                typeString, MessageType.Custom);
            type = MessageType.Custom;
        }

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
