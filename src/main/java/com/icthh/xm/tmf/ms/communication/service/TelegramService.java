package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.dto.KeyboardDto;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.icthh.xm.tmf.ms.communication.config.Constants.REPLY_MARKUP;

@Slf4j
@Service
@IgnoreLogginAspect
@RequiredArgsConstructor
public class TelegramService implements MessageService {

    private final ApplicationProperties applicationProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TelegramBotRegisterService registerService;

    public CommunicationMessage receive(String tenantKey, CommunicationMessage message) {
        final StopWatch stopWatch = StopWatch.createStarted();
        String topicName = buildReciveMessageTopicName(tenantKey);

        log.info("start sending message to processing queue = [{}]", topicName);
        try {
            String data = toJson(message);
            kafkaTemplate.send(buildReciveMessageTopicName(tenantKey), data);
        } finally {
            log.info("stop sending message to processing queue, time = {} ms.", stopWatch.getTime());
        }
        return message;
    }

    @Override
    public CommunicationMessage send(String tenantKey, CommunicationMessageCreate message) {
        String botKey = message.getType();
        Map<String, TelegramBot> tenantsBots = registerService.getTenantBots(tenantKey);
        TelegramBot bot = tenantsBots.get(botKey);
        if (bot == null) {
            log.warn("TelegramBot not found for key: [{}]. Message: [{}] skipped.", botKey, message.getContent());
            return new CommunicationMessage();
        }

        message.getReceiver().forEach(receiver -> botExecute(bot, receiver, message));
        return new CommunicationMessage();
    }

    private void botExecute(TelegramBot bot, Receiver receiver, CommunicationMessageCreate message) {
        final StopWatch stopWatch = StopWatch.createStarted();
        String chatId = receiver.getAppUserId();
        log.info("start sending message, bot = {}, chatId = {}", message.getType(), chatId);
        try {
            SendMessage request = new SendMessage(chatId, message.getContent());
            message.getCharacteristic()
                    .stream()
                    .filter(it -> it.getName().equals(REPLY_MARKUP))
                    .findFirst()
                    .map(CommunicationRequestCharacteristic::getValue)
                    .map(this::fromJson)
                    .map(KeyboardDto::build)
                    .ifPresent(request::replyMarkup);

            bot.execute(request);
            log.info("stop sending message, time = {} ms.", stopWatch.getTime());
        } catch (Exception ex) {
            log.error("error processing message: {}, time = {} ms.", ex.getMessage(), stopWatch.getTime());
        }
    }

    @SneakyThrows
    private <T> String toJson(T message) {
        return objectMapper.writeValueAsString(message);
    }

    @SneakyThrows
    private KeyboardDto fromJson(String value) {
        return objectMapper.readValue(value, KeyboardDto.class);
    }

    private String buildReciveMessageTopicName(String tenantKey) {
        String sendQueuePattern = applicationProperties.getMessaging().getReciveQueueNameTemplate();
        return String.format(sendQueuePattern, tenantKey.toLowerCase(), MessageType.Telegram.name().toLowerCase());
    }
}
