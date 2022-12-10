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

@Component
@RequiredArgsConstructor
public class EmailSpecService extends AbstractRefreshableConfiguration<EmailSpec> {

    private final ApplicationProperties properties;
    private final CustomEmailSpecService customEmailSpecService;
    private final TenantContextHolder tenantContextHolder;

    @LoggingAspectConfig(resultDetails = false)
    public EmailSpec getEmailSpec() {
        String tenantKey = tenantContextHolder.getTenantKey();
        return getEmailSpec(tenantKey);
    }

    @LoggingAspectConfig(resultDetails = false)
    public EmailSpec getEmailSpec(String tenantKey) {
        String cfgTenantKey = tenantKey.toUpperCase();
        if (!getConfigurations().containsKey(cfgTenantKey)) {
            throw new EntityNotFoundException("Email specification not found");
        }

        EmailSpec emailSpec = getConfigurations().get(cfgTenantKey);
        CustomEmailSpec customEmailSpec = customEmailSpecService.getConfigurations().get(cfgTenantKey);
        return emailSpec.override(customEmailSpec);
    }

    public EmailTemplateSpec getEmailTemplateSpec(String tenantKey, String templateKey) {
        List<EmailTemplateSpec> emailTemplateSpecList = getEmailSpec(tenantKey).getEmails();
        return emailTemplateSpecList.stream().filter(it -> it.getTemplateKey().equals(templateKey)).findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Email specification not found"));
    }

    @Override
    public String getConfigPathPattern() {
        return properties.getEmailSpecificationPathPattern();
    }

    @Override
    public Class<EmailSpec> getConfigClass() {
        return EmailSpec.class;
    }
}
