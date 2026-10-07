package com.icthh.xm.tmf.ms.communication.config;

import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.lep.LepKafkaMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingAdapter;
import com.icthh.xm.tmf.ms.communication.messaging.SendToKafkaDeliveryReportListener;
import com.icthh.xm.tmf.ms.communication.messaging.SendToKafkaMoDeliveryReportListener;
import com.icthh.xm.tmf.ms.communication.messaging.ToSendQueueDynamicConsumerConfiguration;
import com.icthh.xm.tmf.ms.communication.messaging.ToSendQueueMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Configures Kafka messaging: the to-send queue consumer and the delivery report producers.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty("application.stream-binding-enabled")
public class MessagingConfiguration {

    @Bean
    public ToSendQueueMessageHandler toSendQueueMessageHandler(ObjectMapper objectMapper,
                                                               MessageHandlerService messageHandlerService,
                                                               LepKafkaMessageHandler lepMessageHandler) {
        return new ToSendQueueMessageHandler(objectMapper, messageHandlerService, lepMessageHandler);
    }

    @Bean
    public ToSendQueueDynamicConsumerConfiguration toSendQueueDynamicConsumerConfiguration(
        ApplicationProperties applicationProperties,
        KafkaProperties kafkaProperties,
        ToSendQueueMessageHandler toSendQueueMessageHandler,
        ApplicationEventPublisher applicationEventPublisher) {
        return new ToSendQueueDynamicConsumerConfiguration(applicationProperties, kafkaProperties,
            toSendQueueMessageHandler, applicationEventPublisher);
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
                new LinkedBlockingQueue<>()));
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
