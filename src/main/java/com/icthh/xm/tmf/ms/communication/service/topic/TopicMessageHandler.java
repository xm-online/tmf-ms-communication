package com.icthh.xm.tmf.ms.communication.service.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.TemplatedEmailMessageHandler;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
public class TopicMessageHandler implements MessageHandler {

    private final TemplatedEmailMessageHandler emailMessageHandler;
    private final ObjectMapper objectMapper;

    public TopicMessageHandler(TemplatedEmailMessageHandler emailMessageHandler,
                               ObjectMapper objectMapper) {
        this.emailMessageHandler = emailMessageHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig) {
        CommunicationMessageCreate communicationMessage = toObject(message);
        emailMessageHandler.handle(communicationMessage);
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
