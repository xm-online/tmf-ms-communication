package com.icthh.xm.tmf.ms.communication.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTopicsConfiguration {

    private final ApplicationProperties applicationProperties;
    private final KafkaProperties kafkaProperties;

    @Bean
    @ConditionalOnProperty(prefix = "application.kafka-topics", name = "auto-create", havingValue = "true")
    public void createTopics() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        String deliveredMoQueueName = applicationProperties.getMessaging().getDeliveredMoQueueName();
        try (AdminClient client = AdminClient.create(configs)) {
            ListTopicsOptions options = new ListTopicsOptions();
            options.listInternal(true);
            ListTopicsResult topics = client.listTopics(options);
            Set<String> currentTopicList = topics.names().get();
            for (ApplicationProperties.KafkaTopics.Config config : applicationProperties.getKafkaTopics().getConfigs()) {
                if (!currentTopicList.contains(config.getName())) {
                    log.info("Creating topic {}", config.getName());
                    NewTopic newTopic = new NewTopic(config.getName(), config.getNumPartitions(),
                        config.getReplicationFactor().shortValue());
                    client.createTopics(List.of(newTopic));
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Cannot create topic {}", deliveredMoQueueName, e);
        }
    }

}
