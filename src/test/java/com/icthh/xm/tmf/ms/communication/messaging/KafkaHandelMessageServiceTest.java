package com.icthh.xm.tmf.ms.communication.messaging;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.service.KafkaHandelMessageService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class KafkaHandelMessageServiceTest {

    public static final int RATE_LIMIT = 1;
    public static final int POOL_SIZE = 10;

    private KafkaHandelMessageService kafkaHandelMessageService;
    private MessagingHandler messagingHandler;
    private ApplicationProperties applicationProperties = createApplicationProperties();

    @Before
    public void setUp() {
        this.messagingHandler = mock(MessagingHandler.class);
        this.kafkaHandelMessageService = new KafkaHandelMessageService(applicationProperties, messagingHandler);
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
    public void testLimitHandler() throws InterruptedException {
        AtomicInteger handlerCount = new AtomicInteger();
        AtomicInteger sleepHandler = new AtomicInteger();
        doAnswer(invocationOnMock -> {
            handlerCount.getAndIncrement();
            return null;
        }).when(messagingHandler).handleEvent(any());

        for (int i = 0; i < 10; i++) {
            kafkaHandelMessageService.handle(new MockMessage());
        }
        do {
            Thread.sleep(1000);
            sleepHandler.getAndIncrement();
            Assert.assertEquals(sleepHandler.get(), handlerCount.get());
        } while (handlerCount.get() != 10);
    }

    private static class MockMessage implements Message<String> {

        @Override
        public String getPayload() {
            return "";
        }

        @Override
        public MessageHeaders getHeaders() {
            return null;
        }
    }
}
