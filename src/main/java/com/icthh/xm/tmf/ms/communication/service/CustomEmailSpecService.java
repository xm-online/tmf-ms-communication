package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomEmailSpecService extends AbstractRefreshableConfiguration<CustomEmailSpec> {

    private final ApplicationProperties properties;

    @Override
    public String getConfigPathPattern() {
        return properties.getCustomEmailSpecificationPathPattern();
    }

    @Override
    public Class<CustomEmailSpec> getConfigClass() {
        return CustomEmailSpec.class;
    }
}
