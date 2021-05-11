package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames;
import com.icthh.xm.tmf.ms.communication.utils.Utils;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adds {@link com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames#BADGE}
 * configuration.
 */
@Component
@Slf4j
class BadgeConfigurator implements MessageConfigurator {
    @Override
    public void apply(BuilderWrapper builder, CommunicationMessage message,
                      Map<String, String> characteristics) {
        String badge = characteristics.get(ParameterNames.BADGE);
        if (badge == null) {
            return;
        }

        int badgeIntValue = Utils.parseIntOrException(badge);

        log.debug("{} parameter is provided, adding to the request {}",
            ParameterNames.BADGE, badgeIntValue);

        builder.getApsBuilder().setBadge(badgeIntValue);
        builder.getAndroidNotificationBuilder().setNotificationCount(badgeIntValue);
        builder.getWebpushNotificationBuilder()
            .setBadge(badge);
    }
}
