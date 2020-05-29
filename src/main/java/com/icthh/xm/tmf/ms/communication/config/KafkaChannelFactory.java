package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.tmf.ms.communication.service.KafkaHandelMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
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
import org.springframework.messaging.SubscribableChannel;

import javax.annotation.PostConstruct;
import java.util.Collections;

import static org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties.StartOffset.earliest;

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
    private final ApplicationProperties applicationProperties;
    private final KafkaProperties kafkaProperties;
    private final KafkaHandelMessageService kafkaHandelMessageService;
    private CompositeHealthIndicator bindersHealthIndicator;
    private KafkaBinderHealthIndicator kafkaBinderHealthIndicator;

    public KafkaChannelFactory(BindingServiceProperties bindingServiceProperties,
                               SubscribableChannelBindingTargetFactory bindingTargetFactory,
                               BindingService bindingService,
                               ApplicationProperties applicationProperties, KafkaProperties kafkaProperties,
                               KafkaMessageChannelBinder kafkaMessageChannelBinder,
                               CompositeHealthIndicator bindersHealthIndicator,
                               KafkaBinderHealthIndicator kafkaBinderHealthIndicator,
                               KafkaHandelMessageService kafkaHandelMessageService) {
        this.bindingServiceProperties = bindingServiceProperties;
        this.bindingTargetFactory = bindingTargetFactory;
        this.bindingService = bindingService;
        this.applicationProperties = applicationProperties;
        this.kafkaProperties = kafkaProperties;
        this.bindersHealthIndicator = bindersHealthIndicator;
        this.kafkaBinderHealthIndicator = kafkaBinderHealthIndicator;
        this.kafkaHandelMessageService = kafkaHandelMessageService;
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
        kafkaExtendedBindingProperties.setBindings(Collections.singletonMap(chanelName, props));

        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setMaxAttempts(Integer.MAX_VALUE);
        consumerProperties.setHeaderMode(HeaderMode.none);
        consumerProperties.setPartitioned(true);
        consumerProperties.setConcurrency(applicationProperties.getKafkaConcurrencyCount());

        BindingProperties bindingProperties = new BindingProperties();
        bindingProperties.setConsumer(consumerProperties);
        bindingProperties.setDestination(chanelName);
        bindingProperties.setGroup(kafkaProperties.getConsumer().getGroupId());
        bindingServiceProperties.setBindings(Collections.singletonMap(chanelName, bindingProperties));

        SubscribableChannel channel = bindingTargetFactory.createInput(chanelName);
        bindingService.bindConsumer(channel, chanelName);

        HealthIndicatorRegistry registry = bindersHealthIndicator.getRegistry();
        if (registry.get(KAFKA) == null) {
            registry.register(KAFKA, kafkaBinderHealthIndicator);
        }
        channel.subscribe(kafkaHandelMessageService::handle);
    }
}
