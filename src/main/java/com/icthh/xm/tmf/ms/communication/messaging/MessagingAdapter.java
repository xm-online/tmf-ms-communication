package com.icthh.xm.tmf.ms.communication.messaging;

import static org.jsmpp.bean.MessageState.DELIVERED;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.DeliveryReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
public class MessagingAdapter {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationProperties applicationProperties;
    private final KafkaProperties kafkaProperties;

    final static int NUM_PARTITIONS = 3;
    final static short REPLICATION_FACTOR = 3;

    @PostConstruct
    public void initTopic() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        String deliveredMoQueueName = applicationProperties.getMessaging().getDeliveredMoQueueName();
        try (AdminClient client = AdminClient.create(configs)) {
            ListTopicsOptions options = new ListTopicsOptions();
            options.listInternal(true);
            ListTopicsResult topics = client.listTopics(options);
            Set<String> currentTopicList = topics.names().get();
            if (!currentTopicList.contains(deliveredMoQueueName)) {
                log.info("Creating topic {}", deliveredMoQueueName);
                NewTopic newTopic = new NewTopic(deliveredMoQueueName, NUM_PARTITIONS, REPLICATION_FACTOR);
                client.createTopics(List.of(newTopic));
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Cannot create topic {}", deliveredMoQueueName, e);
        }
    }

    public void deliveryReport(DeliveryReport deliveryReport) {
        ApplicationProperties.Messaging messaging = applicationProperties.getMessaging();
        String topic = deliveryReport.getDeliveryStatus().equals(DELIVERED.name()) ? messaging.getDeliveredQueueName() :
                       messaging.getDeliveryFailedQueueName();
        kafkaTemplate.send(topic, deliveryReport);
    }

    public void moDeliveryReport(String message) {
        String topic = applicationProperties.getMessaging().getDeliveredMoQueueName();
        kafkaTemplate.send(topic, message);
    }

}
