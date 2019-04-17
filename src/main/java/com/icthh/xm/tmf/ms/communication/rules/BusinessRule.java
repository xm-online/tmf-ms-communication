package com.icthh.xm.tmf.ms.communication.rules;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.springframework.core.Ordered;

public interface BusinessRule extends Ordered {

    void validate(CommunicationMessage message);

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
