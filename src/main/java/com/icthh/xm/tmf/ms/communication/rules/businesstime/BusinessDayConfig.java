package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import lombok.Data;


@Data
class BusinessDayConfig {

    private BusinessTimeConfig businessTime;

    @Data
    public static class BusinessTimeConfig {
        private Map<String, BusinessTime> businessDay;
        private Map<LocalDate, BusinessTime> exceptionDate;
    }

    @Data
    public static class BusinessTime {
        private LocalTime startTime;
        private LocalTime endTime;
    }

    BusinessTime getBusinessTimeConfig(LocalDateTime localDateTime) {
        BusinessTime exceptionDateConfig = getExceptionDateConfig(localDateTime.toLocalDate());

        if (exceptionDateConfig != null) {
            return createBusinessTimeConfig(exceptionDateConfig);
        }
        return createBusinessTimeConfig(getBusinessDayConfig(localDateTime.getDayOfWeek()));

    }

    private BusinessTime getExceptionDateConfig(LocalDate localDate) {
        Map<LocalDate, BusinessTime> exceptionDateConfig = getBusinessTime().getExceptionDate();

        if (exceptionDateConfig != null && exceptionDateConfig.containsKey(localDate)) {
            return exceptionDateConfig.get(localDate);
        }
        return null;
    }

    private BusinessTime getBusinessDayConfig(DayOfWeek dayOfWeek) {
        return getBusinessTime().getBusinessDay().get(dayOfWeek.toString().toLowerCase());
    }

    private BusinessTime createBusinessTimeConfig(BusinessTime businessDayConfig) {
        if (businessDayConfig == null) {
            businessDayConfig = new BusinessTime();
            businessDayConfig.setStartTime(LocalTime.MIN);
            businessDayConfig.setEndTime(LocalTime.MAX);
        } else {
            if (businessDayConfig.getStartTime() == null) {
                businessDayConfig.setStartTime(LocalTime.MIN);
            }
            if (businessDayConfig.getEndTime() == null) {
                businessDayConfig.setEndTime(LocalTime.MIN);
            }
        }
        return businessDayConfig;
    }
}


