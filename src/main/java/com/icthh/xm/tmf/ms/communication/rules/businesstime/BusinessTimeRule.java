package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import com.icthh.xm.tmf.ms.communication.rules.BusinessRule;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig.BusinessTime;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public class BusinessTimeRule implements BusinessRule {

    private static final String NOT_BUSINESS_TIME_CODE = "error.business.sending.notBusinessTime";
    private static final String BUSINESS_TIME = "businessDayConfig";
    private static final String BUSINESS_TIME_RULE = BUSINESS_TIME + "Rule";

    private final BusinessTimeConfigService timeConfigService;
    private final Clock clock;

    @Override
    public RuleResponse validate(CommunicationMessage message) {
        RuleResponse ruleResponse = new RuleResponse();
        ruleResponse.setRuleType(BUSINESS_TIME_RULE);
        ruleResponse.setSuccess(true);
        if (message.getId() != null && !message.getId().isEmpty()) {
            LocalDateTime currentDateTime = LocalDateTime.now(clock);

            BusinessDayConfig businessDayConfig = timeConfigService.getBusinessDayConfig();
            BusinessTime businessTime = businessDayConfig.getCurrentBusinessTime(currentDateTime);

            LocalTime currentTime = currentDateTime.toLocalTime();
            LocalTime startTime = businessTime.getStartTime();
            LocalTime endTime = businessTime.getEndTime();
            log.debug("start time: {}, end time: {}, current time: {}", startTime, endTime, currentTime);

            if (startTime.isAfter(currentTime) || endTime.isBefore(currentTime)) {
                ruleResponse.setSuccess(false);
                ruleResponse.setResponseCode(NOT_BUSINESS_TIME_CODE);
                return ruleResponse;
            }
        }
        return ruleResponse;
    }
}
