package com.icthh.xm.tmf.ms.communication.messaging.template;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.service.mail.MultiTenantLangStringTemplateLoaderService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Slf4j
public abstract class AbstractMessageTemplateService implements MessageTemplateService {

    private static final String TEMPLATE_NAME_MESSAGE_CHARACTERISTIC = "templateName";
    private static final String LANGUAGE_MESSAGE_CHARACTERISTIC = "language";
    private static final String TEMPLATE_MODEL_MESSAGE_CHARACTERISTIC = "templateModel";

    private final Configuration freeMarkerConfiguration;
    private final StringTemplateLoader templateLoader;
    private final MultiTenantLangStringTemplateLoaderService templateLoaderService;
    private ObjectMapper objectMapper;

    protected AbstractMessageTemplateService(freemarker.template.Configuration freeMarkerConfiguration,
                                             StringTemplateLoader templateLoader,
                                             MultiTenantLangStringTemplateLoaderService templateLoaderService) {
        this.freeMarkerConfiguration = freeMarkerConfiguration;
        this.templateLoader = templateLoader;
        this.templateLoaderService = templateLoaderService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getMessageContent(String tenantKey, CommunicationMessageCreate message) {
        if (StringUtils.isNotEmpty(message.getContent())) {
            return message.getContent();
        }
        String messageTemplateName = getMessageTemplateName(message);
        Locale locale = getMessageLocale(message);
        Map<String, Object> model = getMessageModel(message);

        return getMessageContent(tenantKey, messageTemplateName, locale, model);
    }

    /**
     * Method to fill up given template with parameters (e.g. '${param1}', '${dto.param1 + ' ' + dto.param2}')
     * @param tenantKey tenant to get template loader from
     * @param content template content to fill
     * @param model data to fill template with
     * @param lang template language
     * @param templatePath template path
     * @return message content ready for sending
     */
    public String processTemplate(String tenantKey, String content, Map<String, Object> model,
                                  String lang, String templatePath) {
        try {
            freemarker.template.Configuration configuration = (freemarker.template.Configuration) freeMarkerConfiguration.clone();
            configuration.setTemplateLoader(getMultiTemplateLoader(tenantKey, lang));

            Template template = new Template(templatePath, content, configuration);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

        } catch (TemplateException e) {
            log.error("Template could not be rendered with content: {} and model: {} for language: {}.", content,
                model, lang, e);
            throw new RenderTemplateException(e.getMessageWithoutStackTop(), content, model, lang);
        } catch (IOException e) {
            log.error("Template could not be rendered with content: {} and model: {} for language: {}.", content,
                model, lang, e);
            throw new RenderTemplateException(e.getMessage(), content, model, lang);
        }
    }

    private MultiTemplateLoader getMultiTemplateLoader(String tenantKey, String lang) {
        StringTemplateLoader templateLoaderByTenantAndLang = templateLoaderService.getTemplateLoader(tenantKey, lang);
        return new MultiTemplateLoader(new TemplateLoader[]{templateLoaderByTenantAndLang, templateLoader});
    }

    private Map<String, Object> getMessageModel(CommunicationMessageCreate message) {
        return message.getCharacteristic().stream()
            .filter(c -> TEMPLATE_MODEL_MESSAGE_CHARACTERISTIC.equals(c.getName()))
            .findFirst()
            .map(c -> getTemplateModelMap(c.getValue()))
            .orElse(Map.of());
    }

    private Map<String, Object> getTemplateModelMap(String config) {
        try {
            return objectMapper.readValue(config, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error when read template mode map: " + e.getMessage());
        }
    }

    private Locale getMessageLocale(CommunicationMessageCreate message) {
        return message.getCharacteristic().stream()
            .filter(c -> LANGUAGE_MESSAGE_CHARACTERISTIC.equals(c.getName()))
            .findFirst()
            .map(c -> LocaleUtils.toLocale(c.getValue()))
            .orElse(Locale.getDefault());
    }

    private String getMessageTemplateName(CommunicationMessageCreate message) {
        return message.getCharacteristic().stream()
            .filter(c -> TEMPLATE_NAME_MESSAGE_CHARACTERISTIC.equals(c.getName()))
            .findFirst()
            .map(CommunicationRequestCharacteristic::getValue)
            .orElse(StringUtils.EMPTY);
    }
}
