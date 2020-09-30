package com.icthh.xm.tmf.ms.communication.channel.twilio;

import com.icthh.xm.commons.topic.domain.ConsumerHolder;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import com.icthh.xm.tmf.ms.communication.service.TwilioService;
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
public class TwilioChannelHandlerUnitTest {

    private static final String TENANT_KEY = "TEST";

    @Spy
    @InjectMocks
    private TwilioChannelHandler twilioChannelHandler;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private AbstractMessageListenerContainer container;

    @Mock
    private TwilioService twilioService;

    @Before
    public void setUp() {
        ApplicationProperties.Messaging messaging = new ApplicationProperties.Messaging();
        messaging.setReciveQueueNameTemplate("communication_%s_%s_recive");
        messaging.setSendQueueNameTemplate("communication_%s_%s_send");
        when(applicationProperties.getMessaging()).thenReturn(messaging);

        doReturn(container).when(twilioChannelHandler).buildListenerContainer(any(), any());
    }

    @Test
    public void testStartingOfNewTelegramBots() {
        twilioChannelHandler.onRefresh(TENANT_KEY, buildNTwilios(3));

        verify(container, times(1)).start();
        verify(container, times(0)).stop();

        verify(twilioService, times(3)).registerSender(any(), any());

        Map<String, Map<String, ConsumerHolder>> consumers = twilioChannelHandler.getTenantTwilioConsumers();
        assertEquals(1, consumers.keySet().size());
        assertTrue(consumers.containsKey(TENANT_KEY));

        Map<String, ConsumerHolder> holderMap = consumers.get(TENANT_KEY);
        assertEquals(1, holderMap.keySet().size());
        assertTrue(holderMap.containsKey("default"));

        ConsumerHolder holder = holderMap.get("default");
        assertEquals("communication_test_twilio_send", holder.getTopicConfig().getTopicName());
    }

    @Test
    public void testConfigurationNotChanged() {
        twilioChannelHandler.onRefresh(TENANT_KEY, buildNTwilios(1));

        reset(container);

        twilioChannelHandler.onRefresh(TENANT_KEY, buildNTwilios(1));
        verify(container, times(0)).start();
        verify(container, times(0)).stop();
    }

    private CommunicationSpec buildNTwilios(Integer count) {
        CommunicationSpec spec = new CommunicationSpec();
        CommunicationSpec.Channels channels = new CommunicationSpec.Channels();

        IntStream.range(0, count)
            .forEach(i -> {
                CommunicationSpec.Twilio twilio = new CommunicationSpec.Twilio();
                twilio.setAccountSid("test-sid" + i);
                twilio.setAuthToken("test-auth-token" + i);
                twilio.setKey("test-key" + i);
                channels.getTwilio().add(twilio);
            });
        spec.setChannels(channels);
        return spec;
    }
}
