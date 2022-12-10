package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import freemarker.cache.StringTemplateLoader;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import static java.util.Optional.ofNullable;

/**
 * Service for managing email template.
 */
@Slf4j
@Service
public class TenantEmailTemplateService implements RefreshableConfiguration {

    private static final String LANG_KEY = "langKey";
    private static final String TENANT_KEY = "tenantKey";
    private static final String FILE_PATTERN = "/%s.ftl";
    private static final String DEFAULT_LANG_KEY = "en";
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, String> emailTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> customEmailTemplates = new ConcurrentHashMap<>();
    private final String pathPattern;
    private final String customEmailPathPattern;
    private final StringTemplateLoader templateLoader;
    private final EmailSpecService emailSpecService;

    public TenantEmailTemplateService(ApplicationProperties applicationProperties,
                                      StringTemplateLoader templateLoader,
                                      EmailSpecService emailSpecService) {
        this.templateLoader = templateLoader;
        this.pathPattern = applicationProperties.getEmailPathPattern();
        this.customEmailPathPattern = applicationProperties.getCustomEmailPathPattern();
        this.emailSpecService = emailSpecService;
    }

    @LoggingAspectConfig(resultDetails = false)
    public String getEmailTemplate(String tenantKey, String templateName, String langKey) {
        if (StringUtils.isBlank(langKey)) {
            langKey = DEFAULT_LANG_KEY;
        }

        String templatePath = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKey), templateName, langKey);
        return getTemplateOverrideable(templatePath).orElseThrow(() -> new IllegalArgumentException("Email template was not found"));
    }

    @LoggingAspectConfig(resultDetails = false)
    public String getEmailTemplateByKey(String tenantKey, String templateKey) {
        EmailTemplateSpec emailTemplateSpec = emailSpecService.getEmailTemplateSpec(tenantKey, templateKey);
        String templatePath = emailTemplateSpec.getTemplatePath();
        return getTemplateOverrideable(templatePath).orElseThrow(() -> new IllegalArgumentException("Email template was not found"));
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<String> getTemplateOverrideable(String templatePath) {
        if (customEmailTemplates.containsKey(templatePath)) {
            return Optional.of(customEmailTemplates.get(templatePath));
        } else if (emailTemplates.containsKey(templatePath)) {
            return Optional.of(emailTemplates.get(templatePath));
        }

        return Optional.empty();
    }

    @Override
    public void onRefresh(String key, String config) {
        if (matcher.match(pathPattern, key)) {
            updateTemplates(emailTemplates, pathPattern, key, config);
        } else if (matcher.match(customEmailPathPattern, key)) {
            updateTemplates(customEmailTemplates, customEmailPathPattern, key, config);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(pathPattern, updatedKey) || matcher.match(customEmailPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    private void updateTemplates(Map<String, String> emailTemplates, String pathPattern, String key, String config) {
        Map<String, String> pathVariables = matcher.extractUriTemplateVariables(pathPattern, key);
        String templatePath = matcher.extractPathWithinPattern(pathPattern, key);
        String langKey = pathVariables.get(LANG_KEY);
        String templateFileName = String.format(FILE_PATTERN, pathVariables.get(LANG_KEY));
        templatePath = templatePath.substring(0, templatePath.lastIndexOf(templateFileName));
        String tenantKeyValue = pathVariables.get(TENANT_KEY);

        String templateKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKeyValue), templatePath, langKey);

        if (StringUtils.isBlank(config)) {
            emailTemplates.remove(templateKey);
            log.info("Email template '{}' with locale {} for tenant '{}' was removed", templatePath,
                langKey, tenantKeyValue);
        } else {
            emailTemplates.put(templateKey, config);
            log.info("Email template '{}' with locale {} for tenant '{}' was updated", templatePath,
                langKey, tenantKeyValue);
        }

        ofNullable(getEmailTemplate(tenantKeyValue, templatePath, langKey))
            .ifPresentOrElse((cfg) -> templateLoader.putTemplate(templateKey, cfg),
                                () -> templateLoader.removeTemplate(templateKey));
    }

}
