package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static java.time.LocalTime.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig.BusinessTime;
import org.junit.jupiter.api.Test;

/**
 * BusinessTime must keep its explicit (startTime, endTime) constructor: Lombok skips final fields with an initializer
 * when generating an all-args constructor, so annotating this class with @Value degrades it to a no-args constructor
 * without any compiler error. Jackson then silently ignores the yml values and every day becomes MIN..MAX, i.e. always
 * business time. These tests construct BusinessTime directly, so such a change breaks the build here.
 */
public class BusinessDayConfigUnitTest {

    @Test
    public void absentTimesFallBackToWholeDay() {
        BusinessTime businessTime = new BusinessTime(null, null);

        assertEquals(MIN, businessTime.getStartTime());
        assertEquals(MAX, businessTime.getEndTime());
    }

    @Test
    public void presentTimesAreKept() {
        BusinessTime businessTime = new BusinessTime(of(8, 30), of(12, 30, 30));

        assertEquals(of(8, 30), businessTime.getStartTime());
        assertEquals(of(12, 30, 30), businessTime.getEndTime());
    }
}
