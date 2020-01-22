package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class BusinessDayConfig {

    private BusinessTimeConfig businessTime;

    @Value
    public static class BusinessTimeConfig {

        private Map<String, BusinessTime> businessDay;
        private Map<LocalDate, BusinessTime> exceptionDate;
        private List<CommunicationRequestCharacteristic> exceptionCharacteristics;
    }

    @Value
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
