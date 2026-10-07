package com.icthh.xm.tmf.ms.communication.messaging;

import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import com.icthh.xm.commons.topic.service.dto.RefreshDynamicConsumersEvent;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

/**
 * Consumer of the {@code application.messaging.to-send-queue-name} topic.
 * <p>
 * The topic is shared by all tenants (the tenant is taken from the message), so the consumer
 * is registered only once, under the {@value #OWNER_TENANT} tenant key.
 */
@RequiredArgsConstructor
public class ToSendQueueDynamicConsumerConfiguration implements DynamicConsumerConfiguration {

    static final String OWNER_TENANT = "XM";
    private static final String AUTO_OFFSET_RESET_EARLIEST = "earliest";

    private final ApplicationProperties applicationProperties;
    private final KafkaProperties kafkaProperties;
    private final ToSendQueueMessageHandler messageHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public void onReady(ApplicationReadyEvent applicationReadyEvent) {
        applicationEventPublisher.publishEvent(new RefreshDynamicConsumersEvent(this, OWNER_TENANT));
    }

    @Override
    public List<DynamicConsumer> getDynamicConsumers(String tenantKey) {
        if (!OWNER_TENANT.equals(tenantKey)) {
            return List.of();
        }
        DynamicConsumer dynamicConsumer = new DynamicConsumer();
        dynamicConsumer.setConfig(buildTopicConfig());
        dynamicConsumer.setMessageHandler(messageHandler);
        return List.of(dynamicConsumer);
    }

    private TopicConfig buildTopicConfig() {
        String topicName = applicationProperties.getMessaging().getToSendQueueName();
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setKey(topicName);
        topicConfig.setTypeKey(topicName);
        topicConfig.setTopicName(topicName);
        topicConfig.setGroupId(kafkaProperties.getConsumer().getGroupId());
        topicConfig.setAutoOffsetReset(AUTO_OFFSET_RESET_EARLIEST);
        topicConfig.setConcurrency(applicationProperties.getKafkaConcurrencyCount());
        return topicConfig;
    }
}
