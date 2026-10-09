package com.icthh.xm.tmf.ms.communication.messaging.template;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.XmFreeMarkerConfiguration;
import com.icthh.xm.tmf.ms.communication.config.XmFreeMarkerConfiguration.XmFreeMarkerConfigurer;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.service.mail.MultiTenantLangStringTemplateLoaderService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TwilioMessageTemplateServiceUnitTest {

    private static final String TENANT = "TEST";
    private static final String TEMPLATE_NAME = "templateName";
    private static final Locale LOCALE = new Locale("uk","UA");

    private static final String PATH_PATTERN = "/config/tenants/{tenantKey}/communication/twilio/{templateName}/{langKey}.ftl";
    private static final String CONFIG_PATH = "/config/tenants/TEST/communication/twilio/templateName/uk.ftl";
    private static final String CONFIG = "Hello, ${user.firstName + ' ' + user.lastName}! This is your code: ${code}";

    private TwilioMessageTemplateService twilioMessageTemplateService;
    private MessageTemplateConfigurationService messageTemplateConfigurationService;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration freeMarkerConfiguration = buildFreeMarkerConfiguration();
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        MultiTenantLangStringTemplateLoaderService templateLoaderService = new MultiTenantLangStringTemplateLoaderService();

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        messageTemplateConfigurationService = mock(MessageTemplateConfigurationService.class);

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

        assertThrows(IllegalStateException.class, () -> {
            twilioMessageTemplateService.getMessageContent("", TEMPLATE_NAME, LOCALE, model);
        }, "Language key, template name and tenant must be not blank");

        assertThrows(IllegalStateException.class, () -> {
                twilioMessageTemplateService.getMessageContent(TENANT, null, LOCALE, model);
            }, "Language key, template name and tenant must be not blank");
    }

    @Test
    public void getMessageContent_validDataModel_shouldReturnCorrectMessage() throws JacksonException {
        Map<String, Object> model = Map.of(
            "code", "123456789",
            "user", Map.of("firstName", "John", "lastName", "Smith")
        );

        String result = twilioMessageTemplateService.getMessageContent(TENANT, getCommunicationMessage(model));

        assertThat(result).isNotEmpty();
        assertThat(result).isEqualTo("Hello, John Smith! This is your code: 123456789");
    }

    @Test
    public void getMessageContent_templateInstantiatesClass_shouldThrowRenderTemplateException() {
        when(messageTemplateConfigurationService.getTemplateContent(CONFIG_PATH, MessageType.Twilio))
            .thenReturn("<#assign ex = \"freemarker.template.utility.Execute\"?new()>${ex(\"id\")}");

        RenderTemplateException exception = assertThrows(RenderTemplateException.class,
            () -> twilioMessageTemplateService.getMessageContent(TENANT, TEMPLATE_NAME, LOCALE, Map.of()));

        assertThat(exception.getMessage()).contains("not allowed");
    }

    @Test
    public void getMessageContent_renderFails_logDoesNotContainTemplateAndModel() {
        when(messageTemplateConfigurationService.getTemplateContent(CONFIG_PATH, MessageType.Twilio))
            .thenReturn("TEMPLATE_MARKER ${missing.value}");
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractMessageTemplateService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThrows(RenderTemplateException.class, () -> twilioMessageTemplateService.getMessageContent(
                TENANT, TEMPLATE_NAME, LOCALE, Map.of("secret", "SECRET_MARKER")));

            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains("TEMPLATE_MARKER") || message.contains("SECRET_MARKER"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private CommunicationMessageCreate getCommunicationMessage(Map<String, Object> model) throws JacksonException {
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

    private Configuration buildFreeMarkerConfiguration() throws Exception {
        XmFreeMarkerConfigurer configurer = new XmFreeMarkerConfigurer(new StringTemplateLoader());
        configurer.afterPropertiesSet();
        return new XmFreeMarkerConfiguration().freeMarkerConfiguration(configurer);
    }
}
