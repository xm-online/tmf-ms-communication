package com.icthh.xm.tmf.ms.communication.channel.telegram;

import com.icthh.xm.commons.topic.config.MessageListenerContainerBuilder;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.tmf.ms.communication.channel.ChannelHandler;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec.Channels;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec.Telegram;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.service.TelegramBotRegisterService;
import com.icthh.xm.tmf.ms.communication.service.TelegramService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramChannelHandler implements ChannelHandler {

    private static final String DEFAULT_TELEGRAM_CONSUMER_KEY = "default";

    private final ApplicationProperties applicationProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final TelegramService telegramService;
    private final TelegramBotRegisterService registerService;

    @Getter
    private Map<String, Map<String, ConsumerHolder>> tenantTelegramConsumers = new ConcurrentHashMap<>();

    @Override
    public void onRefresh(String tenantKey, CommunicationSpec spec) {
        List<Telegram> botConfigs = Optional.ofNullable(spec)
            .map(CommunicationSpec::getChannels)
            .map(Channels::getTelegram)
            .orElseGet(Collections::emptyList);

        if (CollectionUtils.isEmpty(botConfigs)) {
            stopAllTenantConsumers(tenantKey);
            registerService.unregisterBot(tenantKey);
            return;
        }

        //process channels queues
        processDefaultTelegramConsumer(tenantKey);
        // todo custom queues for telegram bots will be implemented in next releases
        // processCustomTelegramConsumers(tenantKey, botConfigs);

        //register bots
        botConfigs.forEach(botConfig -> registerService.registerBot(tenantKey, botConfig, telegramService::receive));
    }

    private void processDefaultTelegramConsumer(String tenantKey) {
        Map<String, ConsumerHolder> existingConsumers = getTenantConsumers(tenantKey);
        String topicName = buildSendMessageTopicName(tenantKey);
        TopicConfig topicConfig = buildTopicConfig(topicName);

        if (existingConsumers.containsKey(DEFAULT_TELEGRAM_CONSUMER_KEY)) {
            log.info("[{}] Skip consumer configuration due to no changes found: [{}] ", tenantKey, topicConfig);
            return;
        }

        withLog(tenantKey, "startNewDefaultTelegramConsumer", () -> {
            AbstractMessageListenerContainer listener = buildListenerContainer(tenantKey, topicConfig);
            listener.start();
            existingConsumers.put(DEFAULT_TELEGRAM_CONSUMER_KEY, new ConsumerHolder(topicConfig, listener));
            tenantTelegramConsumers.put(tenantKey, existingConsumers);
        }, "{}", topicConfig);
    }

    protected AbstractMessageListenerContainer buildListenerContainer(String tenantKey, TopicConfig topicConfig) {
        KafkaToTelegramMessageHandler messageHandler = new KafkaToTelegramMessageHandler(telegramService);
        return new MessageListenerContainerBuilder(kafkaProperties, kafkaTemplate)
            .build(tenantKey, topicConfig, messageHandler);
    }

    private void stopAllTenantConsumers(String tenantKey) {
        Map<String, ConsumerHolder> existingConsumers = getTenantConsumers(tenantKey);
        Collection<ConsumerHolder> holders = existingConsumers.values();
        withLog(tenantKey, "stopAllTenantTelegramConsumers", () -> {
            holders.forEach(consumerHolder -> stopConsumer(tenantKey, consumerHolder));
            tenantTelegramConsumers.remove(tenantKey);
        }, "[{}]", holders);
    }

    private void stopConsumer(final String tenantKey, final ConsumerHolder consumerHolder) {
        TopicConfig existConfig = consumerHolder.getTopicConfig();
        withLog(tenantKey, "stopConsumer",
            () -> consumerHolder.getContainer().stop(), "{}", existConfig);
    }

    private String buildSendMessageTopicName(String tenantKey) {
        String sendQueuePattern = applicationProperties.getMessaging().getSendQueueNameTemplate();
        return String.format(sendQueuePattern, tenantKey.toLowerCase(), MessageType.Telegram.name().toLowerCase());
    }

    private TopicConfig buildTopicConfig(String topicName) {
        //todo add possibility to configure topic from telegram communication spec
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setRetriesCount(applicationProperties.getMessaging().getRetriesCount());
        topicConfig.setTopicName(topicName);
        return topicConfig;
    }

    private Map<String, ConsumerHolder> getTenantConsumers(String tenantKey) {
        if (tenantTelegramConsumers.containsKey(tenantKey)) {
            return tenantTelegramConsumers.get(tenantKey);
        } else {
            return new ConcurrentHashMap<>();
        }
    }

    private void withLog(String tenant, String command, Runnable action, String logTemplate, Object... params) {
        final StopWatch stopWatch = StopWatch.createStarted();
        log.info("[{}] start: {} " + logTemplate, tenant, command, params);
        action.run();
        log.info("[{}]  stop: {}, time = {} ms.", tenant, command, stopWatch.getTime());
    }
}
