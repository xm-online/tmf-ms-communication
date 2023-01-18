package com.icthh.xm.tmf.ms.communication.service.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.messaging.handler.TemplatedEmailMessageHandler;
import com.icthh.xm.tmf.ms.communication.utils.ExecuteTenantContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TopicMessageHandlerFactory {

    private final TemplatedEmailMessageHandler templatedEmailMessageHandler;
    private final ExecuteTenantContextUtils executeTenantContextUtils;
    private final ObjectMapper objectMapper;

    public TopicMessageHandler createTopicMessageHandler() {
        return new TopicMessageHandler(executeTenantContextUtils, templatedEmailMessageHandler, objectMapper);
    }
}
