package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;

/**
 * Firebase application name selector. Allows overloading default logic
 */
@LepService(group = "service.message.firebase")
public class FirebaseApplicationSelector {

    @LogicExtensionPoint(value = "ResolveApplicationName",  resolver = CustomMessageResolver.class)
    public String resolveApplicationName(CommunicationMessage message) {
        return message.getSender().getId();
    }
}
