package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import freemarker.cache.StringTemplateLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Service for managing email template.
 */
@Slf4j
@Service
public class TenantEmailTemplateService implements RefreshableConfiguration {

    private static final String FILE_NAME = "fileName";
    private static final String LANG_KEY = "langKey";
    private static final String TENANT_NAME = "tenantName";

    private final ConcurrentHashMap<String, String> emailTemplates = new ConcurrentHashMap<>();
    private final Pattern pattern;
    private final StringTemplateLoader templateLoader;

    public TenantEmailTemplateService(ApplicationProperties applicationProperties,
                                      StringTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
        this.pattern = Pattern.compile(applicationProperties.getEmailPathPattern());
    }

    /**
     * Search email template by email template key.
     *
     * @param emailTemplateKey search key
     * @return email template
     */
    @LoggingAspectConfig(resultDetails = false)
    public String getEmailTemplate(String emailTemplateKey) {
        if (!emailTemplates.containsKey(emailTemplateKey)) {
            throw new IllegalArgumentException("Email template was not found");
        }
        return emailTemplates.get(emailTemplateKey);
    }

    @Override
    public void onRefresh(String key, String config) {
        Matcher matcher = pattern.matcher(key);
        if (!matcher.find()) {
            return;
        }
        String tenantKeyValue = matcher.group(TENANT_NAME);
        String langKey = matcher.group(LANG_KEY);
        String templateName = matcher.group(FILE_NAME);

        String templateKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKeyValue),
            templateName, langKey);

        if (StringUtils.isBlank(config)) {
            emailTemplates.remove(templateKey);
            templateLoader.removeTemplate(templateKey);
            log.info("Email template '{}' with locale {} for tenant '{}' was removed", templateName,
                            langKey, tenantKeyValue);
        } else {
            emailTemplates.put(templateKey, config);
            templateLoader.putTemplate(templateKey, config);
            log.info("Email template '{}' with locale {} for tenant '{}' was updated", templateName,
                            langKey, tenantKeyValue);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return pattern.matcher(updatedKey).matches();
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }
}
