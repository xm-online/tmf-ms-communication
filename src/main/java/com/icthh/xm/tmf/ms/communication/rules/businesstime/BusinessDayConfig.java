package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BusinessDayConfig {

    private BusinessTimeConfig businessTime;

    @Data
    public static class BusinessTimeConfig {

        private Map<String, BusinessTime> businessDay;
        private Map<LocalDate, BusinessTime> exceptionDate;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class BusinessTime {

        private LocalTime startTime = LocalTime.MIN;
        private LocalTime endTime = LocalTime.MAX;
    }

    public BusinessTime getCurrentBusinessTime(LocalDateTime dateTime) {
        return getBusinessTime().getExceptionDate()
            .getOrDefault(dateTime.toLocalDate(), getBusinessDayConfig(dateTime.getDayOfWeek()));

    }

    private BusinessTime getBusinessDayConfig(DayOfWeek dayOfWeek) {
        return getBusinessTime().getBusinessDay().get(dayOfWeek.toString().toLowerCase());
    }

}
