package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.RequiredArgsConstructor;

import javax.annotation.PostConstruct;
import java.util.Map;

import static java.util.Optional.ofNullable;

@LepService(group = "service")
@RequiredArgsConstructor
public class MessageHandlerService {

    private final SmppMessagingHandler smppMessagingHandler;
    private final CustomCommunicationMessageHandler customCommunicationMessageHandler;
    private final TwilioMessageHandler twilioMessageHandler;
    private final Optional<MobileAppMessageHandler> mobileAppMessageHandler;

    private Map<String, BasicMessageHandler> messageHandlerMap;

    @PostConstruct
    void init() {
        messageHandlerMap = new HashMap<>();
        messageHandlerMap.put(MessageType.SMS.name(), smppMessagingHandler);
        messageHandlerMap.put(MessageType.Twilio.name(), twilioMessageHandler);
        mobileAppMessageHandler.ifPresent(mobileHandler ->
            messageHandlerMap.put(MessageType.MobileApp.name(), mobileHandler)
        );
    }

    public BasicMessageHandler getHandler(String type) {
        ofNullable(type).orElseThrow(() -> new IllegalArgumentException("Message type must exists"));
        return ofNullable(messageHandlerMap.get(type)).orElse(customCommunicationMessageHandler);
    }

    @LogicExtensionPoint(value = "RetrieveCommunicationMessage")
    public List<CommunicationMessage> retrieveCommunicationMessage(String id) {
        return List.of();
    }

}
