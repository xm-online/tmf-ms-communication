package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(name = "application.businessRule.enableBusinessTimeRule", havingValue = "true")
@Configuration
public class BusinessTimeRuleConfiguration {

    @Bean
    @Qualifier("clock")
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public BusinessTimeRule businessTimeRule(BusinessTimeConfigService tenantConfigService, @Qualifier("clock") Clock clock) {
        return new BusinessTimeRule(tenantConfigService, clock);
    }

}
