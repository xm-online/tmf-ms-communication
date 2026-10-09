package com.icthh.xm.tmf.ms.communication.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.service.dto.RefreshDynamicConsumersEvent;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class ToSendQueueDynamicConsumerConfigurationUnitTest {

    private static final String TOPIC = "communication_to_send_sms";

    @Mock
    private ToSendQueueMessageHandler messageHandler;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ToSendQueueDynamicConsumerConfiguration configuration;

    @BeforeEach
    public void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getMessaging().setToSendQueueName(TOPIC);
        applicationProperties.setKafkaConcurrencyCount(16);
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.getConsumer().setGroupId("communication");

        configuration = new ToSendQueueDynamicConsumerConfiguration(applicationProperties, kafkaProperties,
            messageHandler, applicationEventPublisher);
    }

    @Test
    public void shouldBuildConsumerForOwnerTenant() {
        List<DynamicConsumer> consumers = configuration.getDynamicConsumers("XM");

        assertThat(consumers).hasSize(1);
        assertThat(consumers.get(0).getMessageHandler()).isSameAs(messageHandler);
        TopicConfig config = consumers.get(0).getConfig();
        assertThat(config.getKey()).isEqualTo(TOPIC);
        assertThat(config.getTopicName()).isEqualTo(TOPIC);
        assertThat(config.getGroupId()).isEqualTo("communication");
        assertThat(config.getAutoOffsetReset()).isEqualTo("earliest");
        assertThat(config.getConcurrency()).isEqualTo(16);
        assertThat(config.getDeadLetterQueue()).isNull();
    }

    @Test
    public void shouldBuildEqualConfigOnEveryCallSoRefreshDoesNotRestartConsumer() {
        assertThat(configuration.getDynamicConsumers("XM").get(0).getConfig())
            .isEqualTo(configuration.getDynamicConsumers("XM").get(0).getConfig());
    }

    @Test
    public void shouldNotBuildConsumerForOtherTenants() {
        assertThat(configuration.getDynamicConsumers("DEMO")).isEmpty();
        assertThat(configuration.getDynamicConsumers("xm")).isEmpty();
    }

    @Test
    public void shouldStartConsumerOnApplicationReady() {
        configuration.onReady(null);

        ArgumentCaptor<RefreshDynamicConsumersEvent> captor = ArgumentCaptor.forClass(RefreshDynamicConsumersEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getTenantKey()).isEqualTo("XM");
    }
}
