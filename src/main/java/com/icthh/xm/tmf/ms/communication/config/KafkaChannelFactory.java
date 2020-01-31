package com.icthh.xm.tmf.ms.communication.config;

import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP;
import static org.apache.commons.lang3.StringUtils.unwrap;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.earliest;
import static org.springframework.kafka.support.KafkaHeaders.ACKNOWLEDGMENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingHandler;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;

import java.util.*;
import javax.annotation.PostConstruct;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.binder.kafka.KafkaBinderHealthIndicator;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBindingProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configures Spring Cloud Stream support.
 * <p>
 * See http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/
 * for more information.
 */
@Slf4j
public class KafkaChannelFactory {

    private static final String KAFKA = "kafka";
    private static long SCHEDULED_TIME = 1000;

    private final BindingServiceProperties bindingServiceProperties;
    private final SubscribableChannelBindingTargetFactory bindingTargetFactory;
    private final BindingService bindingService;
    private final KafkaExtendedBindingProperties kafkaExtendedBindingProperties = new KafkaExtendedBindingProperties();
    private final ObjectMapper objectMapper;
    private final ApplicationProperties applicationProperties;
    private final KafkaProperties kafkaProperties;
    private final MessagingHandler messagingHandler;
    private CompositeHealthIndicator bindersHealthIndicator;
    private KafkaBinderHealthIndicator kafkaBinderHealthIndicator;
    private Consumer<Long, String> consumer;

    public KafkaChannelFactory(BindingServiceProperties bindingServiceProperties,
                               SubscribableChannelBindingTargetFactory bindingTargetFactory,
                               BindingService bindingService, ObjectMapper objectMapper,
                               ApplicationProperties applicationProperties, KafkaProperties kafkaProperties,
                               KafkaMessageChannelBinder kafkaMessageChannelBinder, MessagingHandler messagingHandler,
                               CompositeHealthIndicator bindersHealthIndicator,
                               KafkaBinderHealthIndicator kafkaBinderHealthIndicator,
                               Consumer<Long, String> consumer) {
        this.bindingServiceProperties = bindingServiceProperties;
        this.bindingTargetFactory = bindingTargetFactory;
        this.bindingService = bindingService;
        this.objectMapper = objectMapper;
        this.applicationProperties = applicationProperties;
        this.kafkaProperties = kafkaProperties;
        this.messagingHandler = messagingHandler;
        this.bindersHealthIndicator = bindersHealthIndicator;
        this.kafkaBinderHealthIndicator = kafkaBinderHealthIndicator;
        this.consumer = consumer;
        kafkaMessageChannelBinder.setExtendedBindingProperties(kafkaExtendedBindingProperties);
    }

    @PostConstruct
    public void startHandler() {
        new Thread(() -> {
            long sleepTime = 0;
            long sleepStartTime = 0;
            int messagesCount = 0;
            while (true) {
                log.info("last processing parameters: sleepTime: {} ms, realSleepTime {} ms, messageCount: {} messages", sleepTime, System.currentTimeMillis() - sleepStartTime, messagesCount);
                long startTime = System.currentTimeMillis();
                ConsumerRecords<Long, String> consumerRecords = consumer.poll(SCHEDULED_TIME / 2);
                messagesCount = consumerRecords.count();
                if (consumerRecords.count() > 0) {
                    consumerRecords.forEach(consumerRecord -> {
                        log.debug("handler process {}", consumerRecords.count());
                        try {
                            MdcUtils.putRid(MdcUtils.generateRid());
                            handleEvent(consumerRecord.value(), consumerRecord.timestamp());
                        } catch (Exception e) {
                            log.error("error processign event", e);
                            throw e;
                        } finally {
                            MdcUtils.removeRid();
                        }
                    });
                }
                sleepTime = SCHEDULED_TIME - (System.currentTimeMillis() - startTime);
                if (sleepTime > 0) {
                    try {
                        sleepStartTime = System.currentTimeMillis();
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        log.error("error interrupted event, message {}", e.getMessage());
                    }
                }
            }

        }).start();

    }

    private void handleEvent(String payloadString, long kafkaReceivedTimestamp) {
        final StopWatch stopWatch = StopWatch.createStarted();

        try {
            log.info("start processing message, base64 body = {}", payloadString);
            payloadString = unwrap(payloadString, "\"");
            log.info("start processing message, json body = {}", payloadString);
            CommunicationMessage communicationMessage = mapToCommunicationMessage(payloadString);
            addReceivedByChannelCharacteristic(communicationMessage, kafkaReceivedTimestamp);
            messagingHandler.receiveMessage(communicationMessage);
            log.info("stop processing message, time = {}", stopWatch.getTime());
        } catch (Exception e) {
            log.error("Error process event", e);
        }
    }

    private Map<String, Object> getHeaders(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        Map<String, Object> headersForLog = new HashMap<>();
        headersForLog.putAll(headers);
        headersForLog.remove(ACKNOWLEDGMENT);
        return headersForLog;
    }

    @SneakyThrows
    private CommunicationMessage mapToCommunicationMessage(String eventBody) {
        return objectMapper.readValue(eventBody, CommunicationMessage.class);
    }

    /**
     * Since Kafka headers are not accessible from the business rules,
     * move Kafka received timestamp to the communication message characteristics
     */
    private void addReceivedByChannelCharacteristic(CommunicationMessage communicationMessage, long kafkaReceivedTimestamp) {

        communicationMessage.addCharacteristicItem(
            new CommunicationRequestCharacteristic()
                // Rename it to unlink name from source channel
                .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                .value(Long.toString(kafkaReceivedTimestamp)));

    }
}
