package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import freemarker.cache.StringTemplateLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_LANGUAGE;

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
    private final Map<String, String> emailTemplates = new ConcurrentHashMap<>();
    private final Map<String, String> customEmailTemplates = new ConcurrentHashMap<>();
    private final String pathPattern;
    private final String customEmailPathPattern;
    private final StringTemplateLoader templateLoader;
    private final MultiTenantLangStringTemplateLoaderService multiTenantLangStringTemplateLoaderService;
    private final EmailSpecService emailSpecService;

    public TenantEmailTemplateService(ApplicationProperties applicationProperties,
                                      StringTemplateLoader templateLoader,
                                      MultiTenantLangStringTemplateLoaderService multiTenantLangStringTemplateLoaderService,
                                      EmailSpecService emailSpecService) {
        this.templateLoader = templateLoader;
        this.multiTenantLangStringTemplateLoaderService = multiTenantLangStringTemplateLoaderService;
        this.pathPattern = applicationProperties.getEmailPathPattern();
        this.customEmailPathPattern = applicationProperties.getCustomEmailPathPattern();
        this.emailSpecService = emailSpecService;
    }

    @LoggingAspectConfig(resultDetails = false)
    public String getEmailTemplate(String tenantKey, String templatePath, String langKey) {
        return getTemplateOverrideable(tenantKey, templatePath, langKey).orElseThrow(() -> new IllegalArgumentException("Email template was not found"));
    }

    @LoggingAspectConfig(resultDetails = false)
    public String getEmailTemplateByKey(TenantKey tenantKey, String templateKey, String locale) {
        Optional<EmailTemplateSpec> emailTemplateSpec = emailSpecService.getEmailTemplateSpec(tenantKey.getValue(), templateKey);
        if (emailTemplateSpec.isPresent()) {
            String templatePath = emailTemplateSpec.map(EmailTemplateSpec::getTemplatePath)
                .orElseThrow(() -> new EntityNotFoundException("Template path is empty in specification"));

            return getTemplateOverrideable(templatePath)
                .orElseGet(() -> getEmailTemplate(tenantKey.getValue(), templatePath, locale));
        } else {
            return getEmailTemplate(tenantKey.getValue(), templateKey, locale);
        }
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<String> getTemplateOverrideable(String tenantKey, String templatePath, String langKey) {
        if (StringUtils.isBlank(langKey)) {
            langKey = DEFAULT_LANGUAGE;
        }
        String templateKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKey), templatePath, langKey);

        return getTemplateOverrideable(templateKey);
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<String> getTemplateOverrideable(String templatePath) {
        log.trace("Get template overrideable by templatePath. templatePath: {}, emailTemplates: {}, customEmailTemplates {}",
                templatePath, emailTemplates, customEmailTemplates);

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

        String templatePathKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKeyValue), templatePath, langKey);
        List<EmailTemplateSpec> templateSpecKey = emailSpecService.getEmailTemplateSpecByPath(tenantKeyValue, templatePath);

        if (StringUtils.isBlank(config)) {
            emailTemplates.remove(templatePathKey);
            log.info("Email template '{}' with locale {} for tenant '{}' was removed", templatePath,
                langKey, tenantKeyValue);
        } else {
            emailTemplates.put(templatePathKey, config);
            log.info("Email template '{}' with locale {} for tenant '{}' was updated", templatePath,
                langKey, tenantKeyValue);
        }

        getTemplateOverrideable(tenantKeyValue, templatePath, langKey)
            .ifPresentOrElse((cfg) -> {
                    templateLoader.putTemplate(templatePathKey, cfg);
                    templateSpecKey.forEach(emailTemplateSpec ->
                        multiTenantLangStringTemplateLoaderService.putTemplate(emailTemplateSpec.getTemplateKey(), cfg, tenantKeyValue, langKey));
                },
                () -> {
                    templateLoader.removeTemplate(templatePathKey);
                    templateSpecKey.forEach(emailTemplateSpec ->
                        multiTenantLangStringTemplateLoaderService.removeTemplate(emailTemplateSpec.getTemplateKey(), tenantKeyValue, langKey));
            });
    }

}
