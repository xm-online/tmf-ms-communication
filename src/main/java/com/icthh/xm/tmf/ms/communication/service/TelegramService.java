package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.tmf.ms.communication.channel.telegram.TelegramUpdateListener;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec.Telegram;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.pengrad.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@LoggingAspectConfig
@RequiredArgsConstructor
public class TelegramService implements MessageService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private Map<String, Map<String, TelegramBot>> tenantTelegramBots = new ConcurrentHashMap<>();

    public void registerBot(String tenantKey, Telegram botConfig) {
        //todo add verification logic
        TelegramBot bot = new TelegramBot(botConfig.getToken());
        bot.setUpdatesListener(new TelegramUpdateListener(tenantKey, this::recive));

        Map<String, TelegramBot> telegramBots = getTenantBots(tenantKey);

        if (telegramBots.containsKey(botConfig.getKey())) {
            //tbd skip
            return;
        }
        telegramBots.put(botConfig.getKey(), bot);
    }

    public CommunicationMessage recive(String tenantKey, CommunicationMessage message) {
        //todo send to kafka _recive queue
        return null;
    }

    @Override
    public CommunicationMessage send(String tenantKey, CommunicationMessageCreate message) {
        //todo send to telegram using TelegramBot in tenantTelegramBots
        return null;
    }

    private Map<String, TelegramBot> getTenantBots(String tenantKey) {
        if (tenantTelegramBots.containsKey(tenantKey)) {
            return tenantTelegramBots.get(tenantKey);
        } else {
            return new ConcurrentHashMap<>();
        }
    }
}
