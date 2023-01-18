package com.icthh.xm.tmf.ms.communication.service.topic;

import com.icthh.xm.commons.topic.service.DynamicConsumerConfigurationService;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TopicSpecificationConfigurationUnitTest {
    private static final String TENANT_NAME = "XM";
    private static final String TOPIC_PATH = "/config/tenants/XM/communication/topic-spec.yml";
    private static final String TOPIC_PATH_PATTERN = "/config/tenants/{tenantName}/communication/topic-spec.yml";
    private static final String CONFIG = "test";

    private TopicSpecificationConfiguration subject;
    @Mock
    private TopicSpecificationService topicSpecificationService;
    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private DynamicConsumerConfigurationService dynamicConsumerConfigurationService;

    @Before
    public void setUp() {
        when(applicationProperties.getTopicSpecificationPathPattern()).thenReturn(TOPIC_PATH_PATTERN);

        subject = new TopicSpecificationConfiguration(applicationProperties, topicSpecificationService, dynamicConsumerConfigurationService);
    }

    @Test
    public void isListeningConfigurationSuccess() {
        boolean actual = subject.isListeningConfiguration(TOPIC_PATH);

        assertThat(actual).isTrue();
    }

    @Test
    public void isListeningConfigurationWhenFileNotJsonFail() {
        String incorrectFile = "/config/tenants/XM/communication/incorrect.yml";
        boolean actual = subject.isListeningConfiguration(incorrectFile);

        assertThat(actual).isFalse();
    }

    @Test
    public void onRefreshSuccess() {
        subject.onRefresh(TOPIC_PATH, CONFIG);

        verify(topicSpecificationService).processTopicSpecifications(eq(TENANT_NAME), eq(CONFIG));
        verify(dynamicConsumerConfigurationService).refreshDynamicConsumers(eq(TENANT_NAME));
        verifyNoMoreInteractions(topicSpecificationService);
        verifyNoMoreInteractions(dynamicConsumerConfigurationService);
    }
}
