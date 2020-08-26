package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
public class MessageHandlerService {

    private final CustomCommunicationMessageHandler customCommunicationMessageHandler;
    private final Map<MessageType, BasicMessageHandler> messageHandlers;

    public MessageHandlerService(CustomCommunicationMessageHandler customCommunicationMessageHandler, List<BasicMessageHandler> messageHandlers) {
        this.customCommunicationMessageHandler = customCommunicationMessageHandler;
        this.messageHandlers = messageHandlers.stream().collect(Collectors.toMap(BasicMessageHandler::getType, it -> it));
    }

    public BasicMessageHandler getHandler(String type) {
        ofNullable(type).orElseThrow(() -> new IllegalArgumentException("Message type must exists"));
        return ofNullable(messageHandlers.get(MessageType.valueOf(type))).orElse(customCommunicationMessageHandler);
    }

}
