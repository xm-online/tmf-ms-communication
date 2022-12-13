package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailTemplateSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomEmailSpecService extends AbstractRefreshableConfiguration<CustomEmailSpec> {

    private final ApplicationProperties properties;
    private final TenantContextHolder tenantContextHolder;

    @Override
    public String getConfigPathPattern() {
        return properties.getCustomEmailSpecificationPathPattern();
    }

    @Override
    public Class<CustomEmailSpec> getConfigClass() {
        return CustomEmailSpec.class;
    }

    public CustomEmailSpec updateCustomEmailSpec(CustomEmailTemplateSpec customEmailTemplateSpec) {
        CustomEmailSpec customEmailSpec = getConfiguration(tenantContextHolder.getTenantKey());
        if (customEmailSpec == null) {
            customEmailSpec = new CustomEmailSpec(List.of(customEmailTemplateSpec));
        } else {
            CustomEmailSpec finalCustomEmailSpec = customEmailSpec;
            findCustomEmailTemplateSpec(finalCustomEmailSpec, customEmailTemplateSpec.getTemplateKey())
                .ifPresentOrElse((spec) -> spec.setSubjectTemplate(customEmailTemplateSpec.getSubjectTemplate()),
                                 () -> finalCustomEmailSpec.getEmails().add(customEmailTemplateSpec));
        }

        return customEmailSpec;
    }

    private Optional<CustomEmailTemplateSpec> findCustomEmailTemplateSpec(CustomEmailSpec customEmailSpec, String templateKey) {
        return customEmailSpec.getEmails()
            .stream()
            .filter((spec) -> spec.getTemplateKey().equals(templateKey))
            .findFirst();
    }
}
