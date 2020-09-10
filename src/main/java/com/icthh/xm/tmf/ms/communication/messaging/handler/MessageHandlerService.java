package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class MessageHandlerService {

    private final SmppMessagingHandler smppMessagingHandler;
    private final CustomCommunicationMessageHandler customCommunicationMessageHandler;
    private final MobileAppMessageHandler mobileAppMessageHandler;

    private Map<String, BasicMessageHandler> messageHandlerMap;

    @PostConstruct
    void init() {
        messageHandlerMap = Map.of(
            MessageType.SMS.name(), smppMessagingHandler, //todo V: consider refactoring
            MessageType.MobileApp.name(), mobileAppMessageHandler
        );
    }

    public BasicMessageHandler getHandler(String type) {
        ofNullable(type).orElseThrow(() -> new IllegalArgumentException("Message type must exists"));
        return ofNullable(messageHandlerMap.get(type)).orElse(customCommunicationMessageHandler);
    }

}
