package com.icthh.xm.tmf.ms.communication.service.smpp;

import lombok.Getter;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.RelativeTimeFormatter;
import org.jsmpp.util.TimeFormatter;

@Getter
public enum ValidityPeriodType {

    ABSOLUTE(new AbsoluteTimeFormatter()),
    RELATIVE(new RelativeTimeFormatter());

    private final TimeFormatter timeFormatter;

    ValidityPeriodType(TimeFormatter timeFormatter) {
        this.timeFormatter = timeFormatter;
    }
}
