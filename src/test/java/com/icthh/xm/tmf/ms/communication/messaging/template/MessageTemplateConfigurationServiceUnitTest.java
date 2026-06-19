package com.icthh.xm.tmf.ms.communication.messaging.template;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MessageTemplateConfigurationServiceUnitTest {

    private static final String TEST_PATTERN = "/config/tenants/{tenantKey}/communication/twilio/{templateName}/{langKey}.ftl";

    @Mock
    private ApplicationProperties properties;

    private MessageTemplateConfigurationService messageTemplateConfigurationService;

    @BeforeEach
    public void setUp() {
        when(properties.getTwilioPathPattern()).thenReturn(TEST_PATTERN);
        messageTemplateConfigurationService = new MessageTemplateConfigurationService(properties);
    }

    @Test
    public void onInit_shouldUpdateConfigMap() {
        String configKey = "/config/tenants/TEST/communication/twilio/templateName/ua.ftl";
        String config = "Hello, ${user.userKey}!";

        messageTemplateConfigurationService.onInit(configKey, config);
        String result = messageTemplateConfigurationService.getTemplateContent(configKey, MessageType.Twilio);

        assertThat(result).isNotEmpty();
        assertThat(result).isEqualTo(config);
    }
}
