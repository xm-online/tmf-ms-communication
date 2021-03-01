package com.icthh.xm.tmf.ms.communication.rules.ttl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TTLRuleConfiguration {

    @Bean
    public TTLRule ttlRule(TTLRuleConfigService tenantConfigService) {
        return new TTLRule(tenantConfigService);
    }

}
