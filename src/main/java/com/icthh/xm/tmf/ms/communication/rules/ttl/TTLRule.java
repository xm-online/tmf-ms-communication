package com.icthh.xm.tmf.ms.communication.rules.ttl;

import com.icthh.xm.tmf.ms.communication.rules.BusinessRule;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

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
        if (config != null && message.getCharacteristic() != null) {
            config.getTTL().ifPresent((ttl) ->
                message.getCharacteristic().stream()
                    .filter((characteristic) -> MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP.equals(characteristic.getName()))
                    .findFirst()
                    .filter((characteristic) -> characteristic.getValue() != null)
                    .map((characteristic) -> new Date(Long.valueOf(characteristic.getValue())).toInstant())
                    .filter((bornTime) -> Instant.now().minus(ttl).isAfter(bornTime))
                    .ifPresent((bornTime) -> ruleResponse.setSuccess(false))
            );
            if (log.isDebugEnabled()) {
                // do it only for debug mode
                if (config.isActive()) {
                    Duration ttl = config.getTTL().get();
                    message.getCharacteristic().stream()
                        .filter((characteristic) -> MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP.equals(characteristic.getName()))
                        .peek((characteristic) -> log.debug("The following bornTime is found: {}", characteristic.getValue()))
                        .findFirst()
                        .filter((characteristic) -> characteristic.getValue() != null)
                        .map((characteristic) -> new Date(Long.valueOf(characteristic.getValue())).toInstant())
                        .filter((bornTime) -> Instant.now().minus(ttl).isAfter(bornTime))
                        .ifPresent((bornTime) -> log.debug("The following bornTime is rejected: {}", bornTime));
                } else {
                    log.debug("TTLRule is turned off");
                }
            }
        } else {
            log.debug("TTLRule is not configured");
        }
        return ruleResponse;
    }
}
