package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomerEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSpecService extends AbstractRefreshableConfiguration<EmailSpec> {

    private final ApplicationProperties properties;
    private final CustomerEmailSpecService customerEmailSpecService;
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
        CustomerEmailSpec customerEmailSpec = customerEmailSpecService.getConfigurations().get(cfgTenantKey);
        return emailSpec.override(customerEmailSpec);
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
