package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Allows to customize mobile application payload being sent to the mobile device
 */
@Slf4j
@LepService(group = "service.mobileapp")
public class MobileAppMessagePayloadCustomizationService {

    @LogicExtensionPoint("CustomizeMobileAppMessagePayload")
    public Map<String, String> customizePayload(Map<String, String> rawData) {
        log.debug("No-ops payload customizer, returning data from input");
        return rawData;
    }
}
