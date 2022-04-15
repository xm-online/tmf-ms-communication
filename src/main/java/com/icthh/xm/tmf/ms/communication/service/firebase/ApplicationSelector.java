package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.springframework.stereotype.Component;

/**
 * Firebase application name selector. Allows overload default logic (application_name == message.sender.Id ) in LEP
 */
@LepService(group = "service.message")
public class ApplicationSelector {

    @LogicExtensionPoint(value = "GetFirebaseApplicationName",  resolver = CustomMessageResolver.class)
    public String getApplicationName(CommunicationMessage message) {
        return message.getSender().getId();
    }
}
