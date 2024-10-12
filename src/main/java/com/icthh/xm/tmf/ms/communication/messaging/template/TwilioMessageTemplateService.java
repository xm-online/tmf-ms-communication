package com.icthh.xm.tmf.ms.communication.messaging.template;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.service.mail.MultiTenantLangStringTemplateLoaderService;
import freemarker.cache.StringTemplateLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class TwilioMessageTemplateService extends AbstractMessageTemplateService {

    private final ApplicationProperties applicationProperties;
    private final MessageTemplateConfigurationService messageTemplateConfigurationService;

    public TwilioMessageTemplateService(freemarker.template.Configuration freeMarkerConfiguration,
                                        StringTemplateLoader templateLoader,
                                        MultiTenantLangStringTemplateLoaderService templateLoaderService,
                                        ApplicationProperties applicationProperties,
                                        MessageTemplateConfigurationService messageTemplateConfigurationService) {
        super(freeMarkerConfiguration, templateLoader, templateLoaderService);
        this.applicationProperties = applicationProperties;
        this.messageTemplateConfigurationService = messageTemplateConfigurationService;
    }

    @Override
    public String getMessageContent(String tenantKey, String templateName, Locale locale, Map<String, Object> model) {
        String templatePath = getTemplatePath(tenantKey, templateName, locale.getLanguage());
        String templateContent = messageTemplateConfigurationService.getMsisdnTemplateContent(templatePath);
        return processEmailTemplate(tenantKey, templateContent, model, locale.getLanguage(), templatePath);
    }

    private String getTemplatePath(String tenantKey, String templateName, String langKey) {
        if (StringUtils.isBlank(langKey) || StringUtils.isBlank(templateName) || StringUtils.isBlank(tenantKey)) {
            throw new IllegalStateException("Language key, template name and tenant must be not blank");
        }
        return applicationProperties.getMsisdnPathPattern()
            .replace("{tenantKey}", tenantKey)
            .replace("{langKey}", langKey)
            .replace("{templateName}", templateName);
    }
}
