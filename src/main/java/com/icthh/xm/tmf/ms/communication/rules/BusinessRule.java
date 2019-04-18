package com.icthh.xm.tmf.ms.communication.rules;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;

public interface BusinessRule {

    String validate(CommunicationMessage message);

}
