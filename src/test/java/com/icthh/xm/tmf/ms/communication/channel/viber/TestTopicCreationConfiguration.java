package com.icthh.xm.tmf.ms.communication.channel.viber;

import com.google.common.collect.Lists;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@RequiredArgsConstructor
@Order(HIGHEST_PRECEDENCE)
@TestConfiguration
public class TestTopicCreationConfiguration {

    private final KafkaAdmin kafkaAdmin;
    private final ApplicationProperties messaging;

    private List<String> topicsToCreate;

    @PostConstruct
    @SneakyThrows
    private void init() {
        topicsToCreate = Collections.unmodifiableList(Lists.newArrayList(
            messaging.getMessaging().getToSendQueueName(),
            messaging.getMessaging().getSentQueueName(),
            messaging.getMessaging().getSendFailedQueueName(),
            messaging.getMessaging().getDeliveredQueueName(),
            messaging.getMessaging().getDeliveryFailedQueueName())
        );

        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfig())) {
            Set<String> existedTopics = admin
                .listTopics(new ListTopicsOptions().listInternal(true))
                .names()
                .get();
            List<NewTopic> topics = topicsToCreate.stream()
                .filter(topic -> !existedTopics.contains(topic))
                .map(topic ->
                    new NewTopic(topic, 1, (short) 1)
                )
                .collect(Collectors.toList());
            if (!topics.isEmpty()) {
                admin.createTopics(topics).all().get();
            }
        }
    }
}
