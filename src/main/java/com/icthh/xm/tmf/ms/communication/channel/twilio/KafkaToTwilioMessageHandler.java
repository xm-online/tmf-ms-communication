package com.icthh.xm.tmf.ms.communication.channel.twilio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.tmf.ms.communication.service.TwilioService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaToTwilioMessageHandler  implements MessageHandler {

    private final ObjectMapper objectMapper;
    private final TwilioService twilioService;

    public KafkaToTwilioMessageHandler(TwilioService twilioService) {
        this.objectMapper = new ObjectMapper();
        this.twilioService = twilioService;
    }

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig) {
        CommunicationMessage send = twilioService.send(tenant, toObject(message));
        log.info("twilioService: status={} href={}", send.getStatus(), send.getHref());
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
