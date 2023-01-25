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
        return getConfiguration(tenantContextHolder.getTenantKey())
            .map((customSpec) -> {
                findCustomEmailTemplateSpec(customSpec, customEmailTemplateSpec.getTemplateKey())
                    .ifPresentOrElse((foundSpec) -> {
                            foundSpec.updateSubjectTemplate(customEmailTemplateSpec.getSubjectTemplate());
                            foundSpec.updateEmailFrom(customEmailTemplateSpec.getEmailFrom());
                        },
                        () -> customSpec.getEmails().add(customEmailTemplateSpec));
                return customSpec;
            })
            .orElseGet(() -> new CustomEmailSpec(List.of(customEmailTemplateSpec)));
    }

    private Optional<CustomEmailTemplateSpec> findCustomEmailTemplateSpec(CustomEmailSpec customEmailSpec, String templateKey) {
        return customEmailSpec.getEmails()
            .stream()
            .filter((spec) -> spec.getTemplateKey().equals(templateKey))
            .findFirst();
    }
}
