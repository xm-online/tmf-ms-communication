package com.icthh.xm.tmf.ms.communication.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.unwrap;
import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.earliest;
import static org.springframework.kafka.support.KafkaHeaders.ACKNOWLEDGMENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingHandler;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;

/**
 * Configures Spring Cloud Stream support.
 * <p>
 * See http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/
 * for more information.
 */
@Slf4j
public class KafkaChannelFactory {

    private static final String KAFKA = "kafka";

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

    public KafkaChannelFactory(BindingServiceProperties bindingServiceProperties,
                               SubscribableChannelBindingTargetFactory bindingTargetFactory,
                               BindingService bindingService, ObjectMapper objectMapper,
                               ApplicationProperties applicationProperties, KafkaProperties kafkaProperties,
                               KafkaMessageChannelBinder kafkaMessageChannelBinder, MessagingHandler messagingHandler,
                               CompositeHealthIndicator bindersHealthIndicator,
                               KafkaBinderHealthIndicator kafkaBinderHealthIndicator) {
        this.bindingServiceProperties = bindingServiceProperties;
        this.bindingTargetFactory = bindingTargetFactory;
        this.bindingService = bindingService;
        this.objectMapper = objectMapper;
        this.applicationProperties = applicationProperties;
        this.kafkaProperties = kafkaProperties;
        this.messagingHandler = messagingHandler;
        this.bindersHealthIndicator = bindersHealthIndicator;
        this.kafkaBinderHealthIndicator = kafkaBinderHealthIndicator;

        kafkaMessageChannelBinder.setExtendedBindingProperties(kafkaExtendedBindingProperties);
    }

    @PostConstruct
    public void createHandler() {

        String chanelName = applicationProperties.getMessaging().getToSendQueueName();

        KafkaBindingProperties props = new KafkaBindingProperties();
        props.getConsumer().setAutoCommitOffset(false);
        props.getConsumer().setAutoCommitOnError(false);
        props.getConsumer().setStartOffset(earliest);
        props.getConsumer().setAckEachRecord(true);
        kafkaExtendedBindingProperties.getBindings().put(chanelName, props);

        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setMaxAttempts(Integer.MAX_VALUE);
        consumerProperties.setHeaderMode(HeaderMode.none);
        consumerProperties.setPartitioned(true);
        consumerProperties.setConcurrency(applicationProperties.getKafkaConcurrencyCount());

        BindingProperties bindingProperties = new BindingProperties();
        bindingProperties.setConsumer(consumerProperties);
        bindingProperties.setDestination(chanelName);
        bindingProperties.setGroup(kafkaProperties.getConsumer().getGroupId());
        bindingServiceProperties.getBindings().put(chanelName, bindingProperties);

        SubscribableChannel channel = bindingTargetFactory.createInput(chanelName);
        bindingService.bindConsumer(channel, chanelName);

        bindersHealthIndicator.addHealthIndicator(KAFKA, kafkaBinderHealthIndicator);

        channel.subscribe(message -> {
            try {
                MdcUtils.putRid(MdcUtils.generateRid());
                handleEvent(message);
            } catch (Exception e) {
                log.error("error processign event", e);
                throw e;
            } finally {
                MdcUtils.removeRid();
            }
        });

    }

    private void handleEvent(Message<?> message) {
        final StopWatch stopWatch = StopWatch.createStarted();

        // ACKNOWLEDGMENT before processing (important, for avoid duplicate sms)
        message.getHeaders().get(ACKNOWLEDGMENT, Acknowledgment.class).acknowledge();

        try {
            String payloadString = (String) message.getPayload();
            log.info("start processign message, base64 body = {}, headers = {}", payloadString, getHeaders(message));
            payloadString = decodeBase64(payloadString);
            log.info("start processign message, json body = {}", payloadString);
            CommunicationMessageCreate communicationMessage = mapToCommunicationMessage(payloadString);
            messagingHandler.receiveMessage(communicationMessage);
            log.info("stop processign message, time = {}", stopWatch.getTime());
        } catch (Exception e) {
            log.error("Error process event", e);
        }
    }

    private String decodeBase64(String payloadString) {
        try {
            payloadString = unwrap(payloadString, "\"");
            payloadString = new String(Base64.getDecoder().decode(payloadString), UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("Message is not base64", e);
        }
        return payloadString;
    }

    private Map<String, Object> getHeaders(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        Map<String, Object> headersForLog = new HashMap<>();
        headersForLog.putAll(headers);
        headersForLog.remove(ACKNOWLEDGMENT);
        return headersForLog;
    }

    @SneakyThrows
    private CommunicationMessageCreate mapToCommunicationMessage(String eventBody) {
        return objectMapper.readValue(eventBody, CommunicationMessageCreate.class);
    }

}