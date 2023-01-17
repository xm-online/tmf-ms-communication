package com.icthh.xm.tmf.ms.communication.service.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.TopicKafkaQueueParamsSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.TopicSpec;
import com.icthh.xm.tmf.ms.communication.mapper.TopicConfigMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class TopicSpecificationService implements DynamicConsumerConfiguration {

    private final Map<String, DynamicConsumer> dynamicConsumersByTenant = new ConcurrentHashMap<>();
    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
    private final TopicMessageHandler topicMessageHandler;
    private final TopicConfigMapper topicConfigMapper;
    private final String topicNameTemplate;

    public TopicSpecificationService (ApplicationProperties applicationProperties,
                                      TopicMessageHandler topicMessageHandler,
                                      TopicConfigMapper topicConfigMapper) {
        this.topicNameTemplate = applicationProperties.getEmailQueueNameTemplate();
        this.topicMessageHandler = topicMessageHandler;
        this.topicConfigMapper = topicConfigMapper;
    }

    public void processTopicSpecifications(String tenantKey, String config) {
        if (isBlank(config)) {
            dynamicConsumersByTenant.remove(tenantKey);
            return;
        }
        TopicSpec topicSpec = readTopicSpec(config);
        TopicConfig topicConfig = buildTopicConfig(tenantKey, topicSpec.getKafkaQueueParams());
        DynamicConsumer dynamicConsumers = buildDynamicConsumer(topicConfig);

        dynamicConsumersByTenant.put(tenantKey, dynamicConsumers);
    }

    @Override
    public List<DynamicConsumer> getDynamicConsumers(String tenantKey) {
        DynamicConsumer dynamicConsumer = dynamicConsumersByTenant.get(tenantKey);
        return Collections.singletonList(dynamicConsumer);
    }

    @SneakyThrows
    private TopicSpec readTopicSpec(String config) {
        return ymlMapper.readValue(config, TopicSpec.class);
    }

    private DynamicConsumer buildDynamicConsumer(TopicConfig topicConfig) {
        DynamicConsumer dynamicConsumer = new DynamicConsumer();
        dynamicConsumer.setConfig(topicConfig);
        dynamicConsumer.setMessageHandler(topicMessageHandler);
        return dynamicConsumer;
    }

    private TopicConfig buildTopicConfig(String tenantKey, TopicKafkaQueueParamsSpec kafkaQueueParams) {
        TopicConfig topicConfig = topicConfigMapper.topicKafkaQueueParamsToConfig(kafkaQueueParams);
        String topicName = String.format(topicNameTemplate, tenantKey);
        topicConfig.setKey(UUID.randomUUID().toString());
        topicConfig.setTopicName(topicName);
        return topicConfig;
    }
}
