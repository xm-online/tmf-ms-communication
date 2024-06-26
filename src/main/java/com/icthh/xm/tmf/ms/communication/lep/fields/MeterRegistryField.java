package com.icthh.xm.tmf.ms.communication.lep.fields;

import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import io.micrometer.core.instrument.MeterRegistry;

public interface MeterRegistryField extends LepAdditionalContextField {
    String FIELD_NAME = "meterRegistry";
    default MeterRegistry getMeterRegistry() {
        return (MeterRegistry) get(FIELD_NAME);
    }
}
