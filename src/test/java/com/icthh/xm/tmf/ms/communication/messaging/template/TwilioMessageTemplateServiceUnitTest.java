package com.icthh.xm.tmf.ms.communication.messaging.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.AbstractSpringBootTest;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.service.mail.MultiTenantLangStringTemplateLoaderService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import freemarker.cache.StringTemplateLoader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

public class TwilioMessageTemplateServiceUnitTest extends AbstractSpringBootTest {

    private static final String TENANT = "TEST";
    private static final String TEMPLATE_NAME = "templateName";
    private static final Locale LOCALE = new Locale("uk","UA");

    private static final String PATH_PATTERN = "/config/tenants/{tenantKey}/communication/twilio/{templateName}/{langKey}.ftl";
    private static final String CONFIG_PATH = "/config/tenants/TEST/communication/twilio/templateName/uk.ftl";
    private static final String CONFIG = "Hello, ${user.firstName + ' ' + user.lastName}! This is your code: ${code}";

    @Autowired
    private freemarker.template.Configuration freeMarkerConfiguration;

    @Autowired
    private StringTemplateLoader templateLoader;

    @Autowired
    private MultiTenantLangStringTemplateLoaderService templateLoaderService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private MessageTemplateConfigurationService messageTemplateConfigurationService;

    private TwilioMessageTemplateService twilioMessageTemplateService;

    @Before
    public void setUp() {
        when(applicationProperties.getTwilioPathPattern()).thenReturn(PATH_PATTERN);
        when(messageTemplateConfigurationService.getTemplateContent(CONFIG_PATH, MessageType.Twilio)).thenReturn(CONFIG);

        twilioMessageTemplateService = new TwilioMessageTemplateService(freeMarkerConfiguration, templateLoader,
            templateLoaderService, applicationProperties, messageTemplateConfigurationService);
    }

    @Test
    public void getMessageContent_emptyData_shouldThrowException() {
        Map<String, Object> model = Map.of(
            "code", "123456789",
            "user", Map.of("firstName", "John", "lastName", "Smith")
        );

        assertThrows("Language key, template name and tenant must be not blank",
            IllegalStateException.class, () -> {
            twilioMessageTemplateService.getMessageContent("", TEMPLATE_NAME, LOCALE, model);
        });

        assertThrows("Language key, template name and tenant must be not blank",
            IllegalStateException.class, () -> {
                twilioMessageTemplateService.getMessageContent(TENANT, null, LOCALE, model);
            });
    }

    @Test
    public void getMessageContent_validDataModel_shouldReturnCorrectMessage() throws JsonProcessingException {
        Map<String, Object> model = Map.of(
            "code", "123456789",
            "user", Map.of("firstName", "John", "lastName", "Smith")
        );

        String result = twilioMessageTemplateService.getMessageContent(TENANT, getCommunicationMessage(model));

        assertThat(result).isNotEmpty();
        assertThat(result).isEqualTo("Hello, John Smith! This is your code: 123456789");
    }

    private CommunicationMessageCreate getCommunicationMessage(Map<String, Object> model) throws JsonProcessingException {
        CommunicationMessageCreate message = new CommunicationMessageCreate();
        message.setCharacteristic(new ArrayList<>());

        Sender sender = new Sender();
        sender.setId("NOTIFICATION");
        message.setSender(sender);

        addCharacteristic(message, "templateName", TEMPLATE_NAME);
        addCharacteristic(message, "language", LOCALE.getLanguage());
        addCharacteristic(message, "templateModel", new ObjectMapper().writeValueAsString(model));
        return message;
    }

    public CommunicationMessageCreate addCharacteristic(CommunicationMessageCreate message, String name, String value) {
        CommunicationRequestCharacteristic communicationRequestCharacteristic = new CommunicationRequestCharacteristic();
        communicationRequestCharacteristic.setName(name);
        communicationRequestCharacteristic.setValue(value);
        message.getCharacteristic().add(communicationRequestCharacteristic);
        return message;
    }
}
