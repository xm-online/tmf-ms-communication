package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.tmf.ms.communication.channel.telegram.TelegramUpdateListener;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec.Telegram;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.validation.Valid;

@Slf4j
@Service
@IgnoreLogginAspect
@RequiredArgsConstructor
public class TelegramService implements MessageService {

    private final ApplicationProperties applicationProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private Map<String, Map<String, TelegramBot>> tenantTelegramBots = new ConcurrentHashMap<>();

    public void registerBot(String tenantKey, Telegram botConfig) {
        Map<String, TelegramBot> telegramBots = getTenantBots(tenantKey);
        if (telegramBots.containsKey(botConfig.getKey())) {
            log.info("[{}] Skip telegram bot registration because such bot already registered: [{}]",
                tenantKey, botConfig);
            return;
        }

        withLog("startTelegramBot", () -> startTelegramBot(tenantKey, botConfig), botConfig);
    }

    private void startTelegramBot(String tenantKey, Telegram botConfig) {
        TelegramBot bot = new TelegramBot(botConfig.getToken());

        bot.setUpdatesListener(new TelegramUpdateListener(tenantKey, this::recive));

        Map<String, TelegramBot> telegramBots = getTenantBots(tenantKey);
        telegramBots.put(botConfig.getKey(), bot);
        tenantTelegramBots.put(tenantKey, telegramBots);
    }

    public void unregisterBot(String tenantKey) {
        Map<String, TelegramBot> telegramBots = getTenantBots(tenantKey);
        telegramBots.values().forEach(telegramBot ->
            withLog("stopTelegramBot", telegramBot::removeGetUpdatesListener, telegramBot));

        tenantTelegramBots.remove(tenantKey);
    }

    public CommunicationMessage recive(String tenantKey, CommunicationMessage message) {
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
        Map<String, TelegramBot> tenantsBots = getTenantBots(tenantKey);
        TelegramBot bot = tenantsBots.get(botKey);
        if (bot == null) {
            log.warn("TelegramBot not found for key: [{}]. Message: [{}] skipped.", botKey, message.getContent());
            return new CommunicationMessage();
        }

        message.getReceiver().forEach(receiver -> botExecute(bot, receiver, message));
        return new CommunicationMessage();
    }

    protected void botExecute(TelegramBot bot, Receiver receiver, CommunicationMessageCreate message) {
        final StopWatch stopWatch = StopWatch.createStarted();
        String chatId = receiver.getAppUserId();
        log.info("start sending message, bot = {}, chatId = {}", message.getType(), chatId);
        try {
            @Valid List<CommunicationRequestCharacteristic> characteristics = message.getCharacteristic();
            if (characteristics.isEmpty()) {
                bot.execute(new SendMessage(chatId, message.getContent()));
                log.info("stop sending message, time = {} ms.", stopWatch.getTime());
            } else {
                for (CommunicationRequestCharacteristic characteristic : characteristics) {
                    switch (characteristic.getName()) {
                        case "keyboardMarkup":
                            ObjectMapper objectMapper = new ObjectMapper();
                            List<List<LinkedHashMap<String, String>>> keyboardListModel = null;
                            try {
                                keyboardListModel = objectMapper.readValue(characteristic.getValue(), new TypeReference<List<List<LinkedHashMap<String, String>>>>() {});
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String[][] keyboardMarkup = createArray(keyboardListModel);
                            Keyboard replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardMarkup);
                            bot.execute(new SendMessage(chatId, message.getContent())
                                .replyMarkup(replyKeyboardMarkup));
                            break;
                        case "parseMode":
                            bot.execute(new SendMessage(chatId, message.getContent())
                                .parseMode(ParseMode.valueOf(characteristic.getValue())));
                            break;
                        default:
                            bot.execute(new SendMessage(chatId, message.getContent()));
                            log.info("stop sending message, time = {} ms.", stopWatch.getTime());
                            break;
                    }
                }
            }
        } catch (Exception ex) {
            log.error("error processing message: {}, time = {} ms.", ex.getMessage(), stopWatch.getTime());
        }
    }

    private String[][] createArray(List<List<LinkedHashMap<String, String>>> keyboardListModel) {
        return keyboardListModel.stream()
                                    .map(arr -> arr.stream().map(it -> it.get("name")).collect(Collectors.toList()))
                                    .map(arrString -> arrString.toArray(String[]::new))
                                    .toArray(String[][]::new);
    }

    private Map<String, TelegramBot> getTenantBots(String tenantKey) {
        if (tenantTelegramBots.containsKey(tenantKey)) {
            return tenantTelegramBots.get(tenantKey);
        } else {
            return new ConcurrentHashMap<>();
        }
    }

    @SneakyThrows
    private <T> String toJson(T message) {
        return objectMapper.writeValueAsString(message);
    }

    private String buildReciveMessageTopicName(String tenantKey) {
        String sendQueuePattern = applicationProperties.getMessaging().getReciveQueueNameTemplate();
        return String.format(sendQueuePattern, tenantKey.toLowerCase(), MessageType.Telegram.name().toLowerCase());
    }

    private void withLog(String command, Runnable action, Object... params) {
        final StopWatch stopWatch = StopWatch.createStarted();
        log.info("start: {} {}", command, params);
        action.run();
        log.info(" stop: {}, time = {} ms.", command, stopWatch.getTime());
    }
}
