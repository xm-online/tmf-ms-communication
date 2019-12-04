package com.icthh.xm.tmf.ms.communication.channel.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.config.MessageListenerContainerBuilder;
import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.tmf.ms.communication.channel.ChannelHandler;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec.Telegram;
import com.icthh.xm.tmf.ms.communication.service.TelegramService;
import com.icthh.xm.tmf.ms.communication.utils.ApiMapper;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramChannelHandler implements ChannelHandler<Telegram> {

    private static final String TELEGRAM_SEND_QUEUE_TEMPLATE = "communication_%s_telegram_send";
    private static final String TELEGRAM_RECIVE_QUEUE_TEMPLATE = "communication_%s_telegram_recive";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final TelegramService telegramService;

    private Map<String, Map<String, ConsumerHolder>> tenantTelegramConsumers = new ConcurrentHashMap<>();

    @Override
    public void onRefresh(String tenantKey, List<Telegram> telegrams) {
        if (CollectionUtils.isEmpty(telegrams)) {
            log.warn("Skip processing of telegram configuration. Specification is null for tenant: [{}]", tenantKey);
            return;
        }

        //start channels queues
        startTelegramConsumer(tenantKey);

        //register bots
        telegrams.forEach(botConfig -> telegramService.registerBot(tenantKey, botConfig));
    }

    //   @SneakyThrows
    private void startTelegramConsumer(String tenantKey) {
        //todo add verification logic for existing channels
        MessageListenerContainerBuilder builder = new MessageListenerContainerBuilder(kafkaProperties, kafkaTemplate);
        TopicConfig config = new TopicConfig();
        config.setTopicName(String.format(TELEGRAM_SEND_QUEUE_TEMPLATE, tenantKey.toLowerCase()));

        AbstractMessageListenerContainer listener = builder.build(
            tenantKey,
            config,
            (message, tenant, topicConfig) -> telegramService.send(tenantKey, ApiMapper.from(message)));

        //todo verification
        tenantTelegramConsumers.put(tenantKey, Collections.singletonMap("TBD", new ConsumerHolder(config, listener)));
    }

}
