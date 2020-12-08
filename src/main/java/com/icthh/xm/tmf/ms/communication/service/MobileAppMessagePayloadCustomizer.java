package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Allows to customize mobile application payload being sent to the mobile device
 */
@Component
@Slf4j
public class MobileAppMessagePayloadCustomizer {

    @LogicExtensionPoint("CustomizeMobileAppMessagePayload")
    public Map<String, String> customizePayload(Map<String, String> rawData) {
        log.debug("No-ops payload customizer, returning data from input");
        return rawData;
    }
}
