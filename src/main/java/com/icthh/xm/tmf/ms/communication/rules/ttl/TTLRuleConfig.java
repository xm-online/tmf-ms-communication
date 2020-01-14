package com.icthh.xm.tmf.ms.communication.rules.ttl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
        private Action action;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Action {
        NONE(false), REJECT(true), WARNING(true);

        private final boolean active;

        public static Action getDefaultValue(){
            return NONE;
        }
    }

    public boolean isActive(){
        return getAction().isActive();
    }

    public Optional<Duration> getTTL(){
        if (isActive()){
            Duration ttl = Duration.of(ttlRule.value, ChronoUnit.valueOf(ttlRule.chronoUnit));
            return Optional.of(ttl);
        }else{
            return Optional.empty();
        }
    }

    public Action getAction(){
        return (ttlRule == null || ttlRule.action == null) ? Action.getDefaultValue() : ttlRule.action;
    }
}
