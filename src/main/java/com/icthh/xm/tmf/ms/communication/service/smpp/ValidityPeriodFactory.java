package com.icthh.xm.tmf.ms.communication.service.smpp;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import java.util.Date;
import org.apache.commons.validator.routines.IntegerValidator;
import org.springframework.stereotype.Component;

@Component
public class ValidityPeriodFactory {

    private final Integer defaultValidityPeriod;
    private final ValidityPeriodType validityPeriodType;

    public ValidityPeriodFactory(ApplicationProperties applicationProperties) {
        this.validityPeriodType = applicationProperties.getSmpp().getValidityPeriodType();
        this.defaultValidityPeriod = IntegerValidator.getInstance().validate(applicationProperties.getSmpp().getValidityPeriod());
    }

    public String asString(Integer requestedValidityPeriod, Date scheduleDeliveryTime) {
        Integer period = defaultValidityPeriod;
        if (requestedValidityPeriod != null) {
            period = requestedValidityPeriod;
        }
        if (period != null) {
            return validityPeriodType.getTimeFormatter().format(new Date(scheduleDeliveryTime.getTime() + period * 1000));
        }
        return null;
    }
}
