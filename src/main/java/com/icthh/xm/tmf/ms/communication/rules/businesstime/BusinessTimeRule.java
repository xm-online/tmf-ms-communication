package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.icthh.xm.tmf.ms.communication.rules.BusinessRule;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig.BusinessTime;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
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
        if (isNotEmpty(message.getId())) {
            LocalDateTime currentDateTime = LocalDateTime.now(clock);

            BusinessDayConfig businessDayConfig = timeConfigService.getBusinessDayConfig();

            boolean exceptionCharacteristicPresent = false;
            if (businessDayConfig.getBusinessTime() != null) {
                exceptionCharacteristicPresent = isSendingAllowedByCharacteristics(message, businessDayConfig);
            }

            if (!exceptionCharacteristicPresent){
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
        }
        return ruleResponse;
    }

    /**
     * Check whether the message contains characteristics which are allow to send it at not business time
     * @param message Message to check
     * @param businessDayConfig Configuration
     * @return Whether the message contains characteristics which are allow to send it at not business time
     */
    private boolean isSendingAllowedByCharacteristics(CommunicationMessage message, BusinessDayConfig businessDayConfig) {
        boolean exceptionCharacteristicPresent = false;
        List<CommunicationRequestCharacteristic> exceptionCharacteristics = businessDayConfig.getBusinessTime()
            .getExceptionCharacteristics();

        if (exceptionCharacteristics != null && !exceptionCharacteristics.isEmpty() && message.getCharacteristic() != null) {
            exceptionCharacteristicPresent = message.getCharacteristic().stream()
                .anyMatch(exceptionCharacteristics::contains);

            if (exceptionCharacteristicPresent && log.isDebugEnabled()) {
                // Map message to one string for Kibana and like a Json array for pretty look
                log.debug(message.getCharacteristic().stream().filter(exceptionCharacteristics::contains)
                    .map(
                        (characteristic) -> new StringBuilder()
                            .append("{\"name\":\"").append(characteristic.getName()).append("\",")
                            .append("{\"value\":\"").append(characteristic.getValue()).append("\"}")
                    ).collect(
                        Collectors.joining("], [", "The following exceptionCharacteristics are found: [", "]")
                    ));
            }
        }
        return exceptionCharacteristicPresent;
    }
}
