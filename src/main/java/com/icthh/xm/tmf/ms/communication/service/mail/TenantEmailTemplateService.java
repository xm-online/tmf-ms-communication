package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import freemarker.cache.StringTemplateLoader;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

/**
 * Service for managing email template.
 */
@Slf4j
@Service
public class TenantEmailTemplateService implements RefreshableConfiguration {

    private static final String LANG_KEY = "langKey";
    private static final String TENANT_KEY = "tenantKey";
    private static final String FILE_PATTERN = "/%s.ftl";
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, String> emailTemplates = new ConcurrentHashMap<>();
    private final String pathPattern;
    private final StringTemplateLoader templateLoader;

    public TenantEmailTemplateService(ApplicationProperties applicationProperties,
                                      StringTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
        this.pathPattern = applicationProperties.getEmailPathPattern();
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
    @LoggingAspectConfig(resultDetails = false)
    public Optional<String> getTemplateForOverride(String tenantKey, String templatePath, String langKey) {
        String templateKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKey), templatePath, langKey);
        return Optional.ofNullable(getEmailTemplate(templateKey));
    }

    @Override
    public void onRefresh(String key, String config) {
        Map<String, String> pathVariables = matcher.extractUriTemplateVariables(pathPattern, key);
        String templatePath = matcher.extractPathWithinPattern(pathPattern, key);
        String langKey = pathVariables.get(LANG_KEY);
        String templateFileName = String.format(FILE_PATTERN, pathVariables.get(LANG_KEY));
        templatePath = templatePath.substring(0, templatePath.lastIndexOf(templateFileName));
        String tenantKeyValue = pathVariables.get(TENANT_KEY);

        String templateKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKeyValue), templatePath, langKey);

        if (StringUtils.isBlank(config)) {
            emailTemplates.remove(templateKey);
            templateLoader.removeTemplate(templateKey);
            log.info("Email template '{}' with locale {} for tenant '{}' was removed", templatePath,
                langKey, tenantKeyValue);
        } else {
            emailTemplates.put(templateKey, config);
            templateLoader.putTemplate(templateKey, config);
            log.info("Email template '{}' with locale {} for tenant '{}' was updated", templatePath,
                langKey, tenantKeyValue);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(pathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }
}
