package com.icthh.xm.tmf.ms.communication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingHandler;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.stream.binder.kafka.KafkaBinderHealthIndicator;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaExtendedBindingProperties;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingServiceProperties;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP;
import static org.apache.commons.lang3.StringUtils.unwrap;

/**
 * Configures Spring Cloud Stream support.
 * <p>
 * See http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/
 * for more information.
 */
@Slf4j
public class KafkaChannelFactory {

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
    private ConsumerBuilder consumerBuilder;

    public KafkaChannelFactory(BindingServiceProperties bindingServiceProperties,
                               SubscribableChannelBindingTargetFactory bindingTargetFactory,
                               BindingService bindingService, ObjectMapper objectMapper,
                               ApplicationProperties applicationProperties, KafkaProperties kafkaProperties,
                               KafkaMessageChannelBinder kafkaMessageChannelBinder, MessagingHandler messagingHandler,
                               CompositeHealthIndicator bindersHealthIndicator,
                               KafkaBinderHealthIndicator kafkaBinderHealthIndicator,
                               ConsumerBuilder consumerBuilder) {
        this.bindingServiceProperties = bindingServiceProperties;
        this.bindingTargetFactory = bindingTargetFactory;
        this.bindingService = bindingService;
        this.objectMapper = objectMapper;
        this.applicationProperties = applicationProperties;
        this.kafkaProperties = kafkaProperties;
        this.messagingHandler = messagingHandler;
        this.bindersHealthIndicator = bindersHealthIndicator;
        this.kafkaBinderHealthIndicator = kafkaBinderHealthIndicator;
        this.consumerBuilder = consumerBuilder;
        kafkaMessageChannelBinder.setExtendedBindingProperties(kafkaExtendedBindingProperties);
    }

    @PostConstruct
    public void startHandler() {
        new Thread(() -> {
            Consumer<Long, String> consumer = consumerBuilder.buildConsumer();

            while (true) {
                try {
                    long startTime = System.currentTimeMillis();
                    ConsumerRecords<Long, String> consumerRecords;
                    consumerRecords = consumer.poll(Duration.of(applicationProperties.getKafka().getPollDuration(), ChronoUnit.MILLIS));
                    long consumerSleepTime = calculateSleepTimeBetweenMessages(consumerRecords.count());
                    long startHandlingMessageTime = System.currentTimeMillis();
                    final long[] messageParts = {1};
                    if (consumerRecords.count() > 0) {
                        log.debug("handler process {}", consumerRecords.count());
                        consumerRecords.forEach(consumerRecord -> {
                            String value = consumerRecord.value();
                            log.info("start processing message, base64 body = {}", value);
                            value = unwrap(value, "\"");
                            log.info("start processing message, json body = {}", value);
                            CommunicationMessage communicationMessage = mapToCommunicationMessage(value);
                            handleMessage(communicationMessage, consumerRecord.timestamp());
                            messageParts[0] = communicationMessage.getMessageParts();
                            sleep(consumerSleepTime * communicationMessage.getMessageParts(), startHandlingMessageTime);
                        });
                    }
                    sleep(applicationProperties.getKafka().getPeriod() * messageParts[0], startTime);
                } catch (Throwable t) {
                    log.error("Error with message {}", t.getMessage());
                }
            }
        }).start();
    }

    private long calculateSleepTimeBetweenMessages(int messagesCount) {
        if (messagesCount != 0) {
            return applicationProperties.getKafka().getPeriod() / messagesCount;
        }
        return 0;
    }

    private void handleMessage(CommunicationMessage communicationMessage, long kafkaReceivedTimestamp) {
        try {
            MdcUtils.putRid(MdcUtils.generateRid());
            handleEvent(communicationMessage, kafkaReceivedTimestamp);
        } catch (Exception e) {
            log.error("error processign event", e);
            throw e;
        } finally {
            MdcUtils.removeRid();
        }
    }

    private void sleep(long handlingTime, long startTime) {
        long sleep = handlingTime - (System.currentTimeMillis() - startTime);
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                log.error("error interrupted event handling, message {}", e.getMessage());
            }
        }
    }


    private void handleEvent(CommunicationMessage communicationMessage, long kafkaReceivedTimestamp) {
        final StopWatch stopWatch = StopWatch.createStarted();

        try {
            addReceivedByChannelCharacteristic(communicationMessage, kafkaReceivedTimestamp);
            messagingHandler.receiveMessage(communicationMessage);
            log.info("stop processing message, time = {}", stopWatch.getTime());
        } catch (Exception e) {
            log.error("Error process event", e);
        }
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
