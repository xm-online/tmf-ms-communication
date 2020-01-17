package com.icthh.xm.tmf.ms.communication.rules.ttl;

import com.icthh.xm.tmf.ms.communication.rules.BusinessRule;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class TTLRule implements BusinessRule {

    public static final String TTL_EXCEED_MESSAGE_CODE = "error.business.sending.ttlExceeded";
    public static final String MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP = "MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP";
    private static final String TTL_RULE = "TTLRule";

    private final TTLRuleConfigService configService;

    @Override
    public RuleResponse validate(CommunicationMessage message) {
        RuleResponse ruleResponse = new RuleResponse();
        ruleResponse.setRuleType(TTL_RULE);
        ruleResponse.setResponseCode(TTL_EXCEED_MESSAGE_CODE);
        ruleResponse.setSuccess(true);
        TTLRuleConfig config = configService.getTtlRuleConfig();
        try {
            if (config != null && config.isActive() && message.getCharacteristic() != null) {
                config.getTTL().ifPresent((ttl) -> applyRule(message, ttl, ruleResponse));
            } else {
                log.debug("TTLRule is not configured or inactive");
            }
        } catch (Exception e) {
            // in case of any configuration error
            log.error(String.format("Error apply TTL rule: #s", e.getMessage()), e);
        }
        return ruleResponse;
    }

    private void applyRule(CommunicationMessage message, Duration ttl, RuleResponse ruleResponse) {
        message.getCharacteristic().stream()
            .filter(characteristic -> MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP.equals(characteristic.getName()))
            .map(CommunicationRequestCharacteristic::getValue)
            .peek(bornTimestamp -> log.debug("The following bornTime is found: {}", bornTimestamp))
            .findAny()
            .filter(Objects::nonNull)
            .map(Long::valueOf)
            .map(bornTimestamp -> new Date(bornTimestamp).toInstant())
            .filter(bornTime -> Instant.now().minus(ttl).isAfter(bornTime))
            .ifPresent(bornTime -> doAction(bornTime, ruleResponse));
    }

    private void doAction(Instant bornTime, RuleResponse ruleResponse) {
        TTLRuleConfig config = configService.getTtlRuleConfig();
        // checked before, but anyway
        if (config != null) {
            switch (config.getAction()) {
                case REJECT:
                    log.debug("The following bornTime is rejected: {}", bornTime);
                    ruleResponse.setSuccess(false);
                    break;
                case WARNING:
                    log.warn("The TTL is exceeded for the following bornTime: {}", bornTime);
                    break;
                case NONE:
                    // filtered before, but anyway
                    log.debug("TTLRule {} action is configured", TTLRuleConfig.Action.NONE);
                    break;
                default:
                    log.error("The behaviour for the following action value is not implemented: {}", config.getAction());
            }
        } else {
            log.debug("TTLRule is not configured or inactive");
        }
    }
}
