package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Allows customization of message payloads
 */
@Slf4j
@LepService(group = "service.customizer")
public class FirebaseMessagePayloadCustomizationService {

    // todo please revert back old LEP definition for backward compatibility
    @LogicExtensionPoint(value = "CustomizeMessagePayload", resolver = CustomMessageResolver.class)
    public Map<String, String> customizePayload(Map<String, String> rawData, CommunicationMessage message) {
        log.info("No-ops payload customizer, returning data from input");
        return rawData;
    }
}
