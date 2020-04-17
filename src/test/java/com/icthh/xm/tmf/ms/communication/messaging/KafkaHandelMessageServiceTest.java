package com.icthh.xm.tmf.ms.communication.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.lep.LepKafkaMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.service.KafkaHandelMessageService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.message;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.kafka.support.KafkaHeaders.ACKNOWLEDGMENT;

@RunWith(MockitoJUnitRunner.class)
public class KafkaHandelMessageServiceTest {

    public static final int RATE_LIMIT = 1;
    public static final int POOL_SIZE = 10;

    private KafkaHandelMessageService kafkaHandelMessageService;
    @Mock
    private MessageHandlerService messagingHandler;
    @Mock
    private LepKafkaMessageHandler lepMessageHandler;
    @Mock
    ObjectMapper objectMapper;
    private ApplicationProperties applicationProperties = createApplicationProperties();

    @Before
    public void setUp() {
        this.kafkaHandelMessageService = new KafkaHandelMessageService(applicationProperties, messagingHandler,
            lepMessageHandler, objectMapper);
    }

    public static ApplicationProperties createApplicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Kafka kafka = new ApplicationProperties.Kafka();
        applicationProperties.setKafka(kafka);
        kafka.setPoolSize(POOL_SIZE);
        kafka.setRateLimit(RATE_LIMIT);
        return applicationProperties;
    }

    @Test
    public void testLimitHandler() throws InterruptedException, IOException {
        when(objectMapper.readValue(anyString(), eq(CommunicationMessage.class))).thenReturn(message());
        AtomicInteger handlerCount = new AtomicInteger();
        AtomicInteger sleepHandler = new AtomicInteger();
        doAnswer(invocationOnMock -> {
            handlerCount.getAndIncrement();
            return null;
        }).when(messagingHandler).getHandler(any());

        for (int i = 0; i < 10; i++) {
            kafkaHandelMessageService.handle(new MockMessage());
        }
        //sleep for excluding race conditional between start messages processing and test checker;
        Thread.sleep(1200);
        do {
            Assert.assertEquals(sleepHandler.incrementAndGet(), handlerCount.get());
            Thread.sleep(1000);
        } while (handlerCount.get() != 10);
    }

    private static class MockMessage implements Message<String> {

        @Override
        public String getPayload() {
            return "";
        }

        @Override
        public MessageHeaders getHeaders() {
            return new MessageHeaders(Map.of(ACKNOWLEDGMENT, (Acknowledgment) () -> {}));
        }
    }
}
