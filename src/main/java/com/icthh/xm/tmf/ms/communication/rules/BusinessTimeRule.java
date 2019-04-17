package com.icthh.xm.tmf.ms.communication.rules;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private static final String CONFIG_ERROR_MESSAGE = "Cannot get tenant config for";

    private final TenantConfigService tenantConfigService;
    private final Clock clock;

    @Override
    public void validate(CommunicationMessage message) {
        if (message.getId() != null && !message.getId().isEmpty()) {
            LocalDateTime currentDateTime = LocalDateTime.now(clock);
            Map businessDayConfig = getBusinessDayConfig(currentDateTime);

            LocalTime currentTime = currentDateTime.toLocalTime();
            LocalTime startTime = getBusinessTime(businessDayConfig, START_TIME);
            LocalTime endTime = getBusinessTime(businessDayConfig, END_TIME);
            log.debug("start time: {}, end time: {}, current time: {}", startTime, endTime, currentTime);

            if (startTime.toSecondOfDay() > currentTime.toSecondOfDay()
                || endTime.toSecondOfDay() < currentTime.toSecondOfDay()) {
                throw new BusinessException(NOT_BUSINESS_TIME_CODE);
            }
        }
    }

    private Map getBusinessDayConfig(LocalDateTime localDateTime) {
        String currentDay = localDateTime.getDayOfWeek().name().toLowerCase();
        String currentDate = localDateTime.toLocalDate().toString();
        log.debug("current date: {}, current day of week: {}, ", currentDate, currentDay);

        Map businessConfig = ofNullable(tenantConfigService.getConfig().get(BUSINESS_TIME)).map(Map.class::cast)
            .orElseThrow(() -> new IllegalStateException(CONFIG_ERROR_MESSAGE + " check business time"));
        log.debug("business time config: {}", businessConfig);

        Map exceptionDaysConfig = ofNullable(businessConfig.get(EXCEPTION_DAYS)).map(Map.class::cast)
            .orElseThrow(() -> new IllegalStateException(CONFIG_ERROR_MESSAGE + " exception business days"));

        if (exceptionDaysConfig.containsKey(currentDate)) {
            return ofNullable(exceptionDaysConfig.get(currentDate)).map(Map.class::cast)
                .orElseThrow(() -> new IllegalStateException(CONFIG_ERROR_MESSAGE + " business date: " + currentDate));
        }
        return of(businessConfig).map(config -> config.get(currentDay)).map(Map.class::cast)
            .orElseThrow(() -> new IllegalStateException(CONFIG_ERROR_MESSAGE + " business day: " + currentDay));
    }

    private LocalTime getBusinessTime(Map businessDayConfig, String timeType) {
        return ofNullable(businessDayConfig.get(timeType)).map(CharSequence.class::cast)
            .map(LocalTime::parse)
            .orElseThrow(() -> new IllegalStateException(CONFIG_ERROR_MESSAGE + " " + timeType));
    }
}
