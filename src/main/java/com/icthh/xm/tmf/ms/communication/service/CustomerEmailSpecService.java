package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomerEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomerEmailTemplateSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerEmailSpecService extends AbstractRefreshableConfiguration<CustomerEmailSpec> {

    private final ApplicationProperties properties;

    @Override
    public String getConfigPathPattern() {
        return properties.getCustomEmailSpecificationPathPattern();
    }

    @Override
    public Class<CustomerEmailSpec> getConfigClass() {
        return CustomerEmailSpec.class;
    }
}
