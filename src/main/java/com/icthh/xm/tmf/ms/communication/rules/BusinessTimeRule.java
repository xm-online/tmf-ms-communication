package com.icthh.xm.tmf.ms.communication.rules;

import static java.util.Optional.of;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BusinessTimeRule implements BusinessRule {

    private static final String NOT_BUSINESS_TIME_CODE = "error.business.sending.notBusinessTime";
    private static final String BUSINESS_TIME = "businessTime";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String EXCEPTION_DAYS = "exception";

    private final TenantConfigService tenantConfigService;
    private final Clock clock;

    @Override
    public String validate(CommunicationMessage message) {
        if (message.getId() != null && !message.getId().isEmpty()) {
            LocalDateTime currentDateTime = LocalDateTime.now(clock);
            Map<String, Object> businessDayConfig = getBusinessDayConfig(currentDateTime);

            LocalTime currentTime = currentDateTime.toLocalTime();
            LocalTime startTime = getBusinessTime(businessDayConfig, START_TIME);
            LocalTime endTime = getBusinessTime(businessDayConfig, END_TIME);
            log.debug("start time: {}, end time: {}, current time: {}", startTime, endTime, currentTime);

            if (startTime.isAfter(currentTime) || endTime.isBefore(currentTime)) {
                return NOT_BUSINESS_TIME_CODE;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getBusinessDayConfig(LocalDateTime localDateTime) {
        String currentDay = localDateTime.getDayOfWeek().name().toLowerCase();
        String currentDate = localDateTime.toLocalDate().toString();
        log.debug("current date: {}, current day of week: {}, ", currentDate, currentDay);

        if (tenantConfigService.getConfig().get(BUSINESS_TIME) != null) {
            Map<String, Object> businessConfig = of(tenantConfigService.getConfig().get(BUSINESS_TIME))
                                                                       .map(Map.class::cast).get();
            log.debug("business time config: {}", businessConfig);
            if (businessConfig.get(EXCEPTION_DAYS) != null) {
                Map<String, Object> exceptionDaysConfig = of(businessConfig.get(EXCEPTION_DAYS))
                                                                           .map(Map.class::cast).get();
                if (exceptionDaysConfig.containsKey(currentDate)) {
                    return of(exceptionDaysConfig.get(currentDate)).map(Map.class::cast).get();
                }
            }
           return of(businessConfig).map(config -> config.get(currentDay))
                                    .map(Map.class::cast)
                                    .orElse(new HashMap<>());
        }
        return new HashMap<>();
    }

    private LocalTime getBusinessTime(Map<String, Object> businessDayConfig, String timeType) {
        if (businessDayConfig.get(timeType) != null && !businessDayConfig.get(timeType).toString().isEmpty()) {
            return of(businessDayConfig.get(timeType)).map(CharSequence.class::cast)
                                                      .map(LocalTime::parse).get();
        }
        return timeType.equals(START_TIME) ? LocalTime.MIN : LocalTime.MAX;
    }
}
