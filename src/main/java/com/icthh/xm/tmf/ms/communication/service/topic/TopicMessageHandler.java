package com.icthh.xm.tmf.ms.communication.service.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.utils.ExecuteTenantContextUtils;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TopicMessageHandler implements MessageHandler {

    private final ExecuteTenantContextUtils executeTenantContextUtils;
    private final MessageHandlerService messageHandlerService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig) {
        CommunicationMessageCreate communicationMessage = toObject(message);
        executeTenantContextUtils.runInTenantContext(tenant,
            () -> messageHandlerService.getHandler(communicationMessage.getType()).handle(communicationMessage)
        );
    }

    private CommunicationMessageCreate toObject(String message) {
        try {
            return objectMapper.readValue(message, CommunicationMessageCreate.class);
        } catch (Exception ex) {
            log.error("Can't convert message = [{}]", message, ex);
            throw new IllegalArgumentException(ex);
        }
    }
}
