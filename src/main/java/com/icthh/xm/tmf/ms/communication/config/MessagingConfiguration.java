package com.icthh.xm.tmf.ms.communication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingAdapter;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingHandler;
import com.icthh.xm.tmf.ms.communication.messaging.SendToKafkaDeliveryReportListener;
import com.icthh.xm.tmf.ms.communication.messaging.SendToKafkaMoDeliveryReportListener;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.service.SmppService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.kafka.KafkaBinderHealthIndicator;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.cloud.stream.binder.kafka.config.KafkaBinderConfiguration;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Configures Spring Cloud Stream support.
 * <p>
 * See http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/
 * for more information.
 */
@Slf4j
@EnableBinding
@EnableIntegration
@RequiredArgsConstructor
@Import({KafkaBinderConfiguration.class})
@ConditionalOnProperty("application.stream-binding-enabled")
public class MessagingConfiguration {

    @Bean
    public MessagingHandler messagingHandler(KafkaTemplate<String, Object> channelResolver, SmppService smppService,
                                             ApplicationProperties applicationProperties,
                                             BusinessRuleValidator businessRuleValidator) {
        return new MessagingHandler(channelResolver, smppService, applicationProperties, businessRuleValidator);
    }

    @Bean
    public KafkaChannelFactory kafkaChannelFactory(BindingServiceProperties bindingServiceProperties,
                                                   SubscribableChannelBindingTargetFactory bindingTargetFactory,
                                                   BindingService bindingService, ObjectMapper objectMapper,
                                                   ApplicationProperties applicationProperties,
                                                   KafkaMessageChannelBinder kafkaMessageChannelBinder,
                                                   KafkaProperties kafkaProperties, MessagingHandler messageHandler,
                                                   CompositeHealthIndicator bindersHealthIndicator,
                                                   KafkaBinderHealthIndicator kafkaBinderHealthIndicator) {
        return new KafkaChannelFactory(bindingServiceProperties, bindingTargetFactory, bindingService, objectMapper,
            applicationProperties, kafkaProperties, kafkaMessageChannelBinder,
            messageHandler, bindersHealthIndicator, kafkaBinderHealthIndicator);
    }

    @Bean
    public MessagingAdapter messagingAdapter(KafkaTemplate<String, Object> channelResolver,
                                             ApplicationProperties applicationProperties) {
        return new MessagingAdapter(channelResolver, applicationProperties);
    }

    @Bean
    public SendToKafkaDeliveryReportListener deliveryReportListener(MessagingAdapter messagingAdapter,
                                                                    ApplicationProperties applicationProperties) {
        int deliveryProcessorThreadCount = applicationProperties.getMessaging().getDeliveryProcessorThreadCount();
        int deliveryMessageQueueMaxSize = applicationProperties.getMessaging().getDeliveryMessageQueueMaxSize();
        return new SendToKafkaDeliveryReportListener(messagingAdapter,
            new ThreadPoolExecutor(deliveryProcessorThreadCount,
                deliveryMessageQueueMaxSize, 0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()),
            applicationProperties.isConvertToHexDeliveredId());
    }

    @Bean
    public SendToKafkaMoDeliveryReportListener deliveryMoReportListener(MessagingAdapter messagingAdapter,
                                                                        ApplicationProperties applicationProperties) {
        int deliveryProcessorThreadCount = applicationProperties.getMessaging().getDeliveryProcessorThreadCount();
        int deliveryMessageQueueMaxSize = applicationProperties.getMessaging().getDeliveryMessageQueueMaxSize();
        return new SendToKafkaMoDeliveryReportListener(messagingAdapter,
            new ThreadPoolExecutor(deliveryProcessorThreadCount,
                deliveryMessageQueueMaxSize, 0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()));
    }

}
