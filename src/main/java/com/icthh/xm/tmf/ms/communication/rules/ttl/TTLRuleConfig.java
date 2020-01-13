package com.icthh.xm.tmf.ms.communication.rules.ttl;

import lombok.Value;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Value
public class TTLRuleConfig {

    private TTLConfiguration ttlRule;

    @Value
    public static class TTLConfiguration {
        private Long value;
        private String chronoUnit;
    }
    public boolean isActive(){
        return ttlRule != null
            && ttlRule.value != null
            && ttlRule.value > 0;
    }

    public Optional<Duration> getTTL(){
        if (isActive()){
            Duration ttl = Duration.of(ttlRule.value, ChronoUnit.valueOf(ttlRule.chronoUnit));
            return Optional.of(ttl);
        }else{
            return Optional.empty();
        }
    }
}
