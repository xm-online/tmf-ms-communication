package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailSpecService extends AbstractRefreshableConfiguration<EmailSpec> {

    private final ApplicationProperties properties;
    private final CustomEmailSpecService customEmailSpecService;
    private final TenantContextHolder tenantContextHolder;

    @LoggingAspectConfig(resultDetails = false)
    public EmailSpec getEmailSpec() {
        String tenantKey = tenantContextHolder.getTenantKey();
        return getEmailSpec(tenantKey).orElseThrow(() -> new EntityNotFoundException("Email specification not found"));
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<EmailSpec> getEmailSpec(String tenantKey) {
        String cfgTenantKey = tenantKey.toUpperCase();
        if (!getConfigurations().containsKey(cfgTenantKey)) {
            return Optional.empty();
        }

        EmailSpec emailSpec = getConfigurations().get(cfgTenantKey);
        CustomEmailSpec customEmailSpec = customEmailSpecService.getConfigurations().get(cfgTenantKey);
        return Optional.of(emailSpec.override(customEmailSpec));
    }

    public Optional<EmailTemplateSpec> getEmailTemplateSpec(String tenantKey, String templateKey) {
        return getEmailSpec(tenantKey)
            .map(EmailSpec::getEmails)
            .flatMap(emailTemplateSpecList ->
                emailTemplateSpecList.stream().filter(it -> it.getTemplateKey().equals(templateKey)).findFirst());
    }

    public Optional<EmailTemplateSpec> getEmailTemplateSpecByPath(String tenantKey, String templatePath) {
        return getEmailSpec(tenantKey)
            .map(EmailSpec::getEmails)
            .flatMap(emailTemplateSpecList ->
                emailTemplateSpecList.stream().filter(it -> it.getTemplatePath().equals(templatePath)).findFirst());
    }

    @Override
    public String getConfigPathPattern() {
        return properties.getEmailSpecificationPathPattern();
    }

    @Override
    public Class<EmailSpec> getConfigClass() {
        return EmailSpec.class;
    }

    public EmailTemplateSpec getEmailTemplateSpecByKey(String templateKey) {
        String tenantKey = tenantContextHolder.getTenantKey();
        return getEmailTemplateSpec(tenantKey, templateKey)
            .orElseThrow(() -> new EntityNotFoundException("Email template specification not found"));
    }
}
