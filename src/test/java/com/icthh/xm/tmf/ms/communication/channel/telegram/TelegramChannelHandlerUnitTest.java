package com.icthh.xm.tmf.ms.communication.channel.telegram;

import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import com.icthh.xm.tmf.ms.communication.service.TelegramService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TelegramChannelHandlerUnitTest {

    private static final String TENANT_KEY = "TEST";

    @Spy
    @InjectMocks
    private TelegramChannelHandler telegramChannelHandler;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private AbstractMessageListenerContainer container;

    @Mock
    private TelegramService telegramService;

    @Before
    public void setUp() {
        ApplicationProperties.Messaging messaging = new ApplicationProperties.Messaging();
        messaging.setReciveQueueNameTemplate("communication_%s_%s_recive");
        messaging.setSendQueueNameTemplate("communication_%s_%s_send");
        when(applicationProperties.getMessaging()).thenReturn(messaging);

        doReturn(container).when(telegramChannelHandler).buildListenerContainer(any(), any());
    }

    @Test
    public void testStartingOfNewTelegramBots() {
        telegramChannelHandler.onRefresh(TENANT_KEY, buildNBots(3));

        verify(container, times(1)).start();
        verify(container, times(0)).stop();

        verify(telegramService, times(3)).registerBot(any(), any());

        Map<String, Map<String, ConsumerHolder>> consumers = telegramChannelHandler.getTenantTelegramConsumers();
        assertEquals(1, consumers.keySet().size());
        assertTrue(consumers.containsKey(TENANT_KEY));

        Map<String, ConsumerHolder> holderMap = consumers.get(TENANT_KEY);
        assertEquals(1, holderMap.keySet().size());
        assertTrue(holderMap.containsKey("default"));

        ConsumerHolder holder = holderMap.get("default");
        assertEquals("communication_test_telegram_send", holder.getTopicConfig().getTopicName());
    }

    @Test
    public void testConfigurationNotChanged() {
        telegramChannelHandler.onRefresh(TENANT_KEY, buildNBots(1));

        reset(container);

        telegramChannelHandler.onRefresh(TENANT_KEY, buildNBots(1));
        verify(container, times(0)).start();
        verify(container, times(0)).stop();
    }

    private CommunicationSpec buildNBots(Integer count) {
        CommunicationSpec spec = new CommunicationSpec();
        CommunicationSpec.Channels channels = new CommunicationSpec.Channels();

        IntStream.range(0, count)
            .forEach(i -> {
                CommunicationSpec.Telegram telegram = new CommunicationSpec.Telegram();
                telegram.setToken("test-token" + i);
                telegram.setKey("test-key" + i);
                channels.getTelegram().add(telegram);
            });
        spec.setChannels(channels);
        return spec;
    }
}
