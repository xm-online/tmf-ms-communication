package com.icthh.xm.tmf.ms.communication.rules;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(name = "application.businessRule.enableBusinessTimeRule", havingValue = "true")
@Configuration
public class BusinessTimeRuleConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public BusinessTimeRule businessTimeRule(TenantConfigService tenantConfigService, Clock clock) {
        return new BusinessTimeRule(tenantConfigService, clock);
    }

}
