package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.channel.telegram.TelegramUpdateListener;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec.Telegram;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class TelegramBotRegisterService {

    private Map<String, Map<String, TelegramBot>> tenantTelegramBots = new ConcurrentHashMap<>();

    public void registerBot(String tenantKey, Telegram botConfig, BiConsumer<String, CommunicationMessage> receiver) {
        Map<String, TelegramBot> telegramBots = getTenantBots(tenantKey);
        if (telegramBots.containsKey(botConfig.getKey())) {
            log.info("[{}] Skip telegram bot registration because such bot already registered: [{}]",
                    tenantKey, botConfig);
            return;
        }

        withLog("startTelegramBot", () -> startTelegramBot(tenantKey, botConfig, receiver), botConfig);
    }

    private void startTelegramBot(String tenantKey,
                                  Telegram botConfig,
                                  BiConsumer<String, CommunicationMessage> receiver) {
        TelegramBot bot = new TelegramBot(botConfig.getToken());

        bot.setUpdatesListener(new TelegramUpdateListener(tenantKey, receiver));

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

    public Map<String, TelegramBot> getTenantBots(String tenantKey) {
        if (tenantTelegramBots.containsKey(tenantKey)) {
            return tenantTelegramBots.get(tenantKey);
        } else {
            return new ConcurrentHashMap<>();
        }
    }


    private void withLog(String command, Runnable action, Object... params) {
        final StopWatch stopWatch = StopWatch.createStarted();
        log.info("start: {} {}", command, params);
        action.run();
        log.info(" stop: {}, time = {} ms.", command, stopWatch.getTime());
    }
}
