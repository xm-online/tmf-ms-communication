package com.icthh.xm.tmf.ms.communication.rules;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty
@Configuration
public class BusinessTimeRuleConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

}
