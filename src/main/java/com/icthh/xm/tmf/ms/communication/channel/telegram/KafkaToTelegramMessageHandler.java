package com.icthh.xm.tmf.ms.communication.channel.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.tmf.ms.communication.service.TelegramService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaToTelegramMessageHandler implements MessageHandler {

    private final ObjectMapper objectMapper;
    private final TelegramService telegramService;

    public KafkaToTelegramMessageHandler(TelegramService telegramService) {
        this.telegramService = telegramService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onMessage(String messageString, String tenant, TopicConfig topicConfig) {
        CommunicationMessageCreate message = toObject(messageString);
        telegramService.send(tenant, message);
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
