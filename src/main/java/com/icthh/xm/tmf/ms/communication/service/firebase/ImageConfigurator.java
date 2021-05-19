package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Adds {@link com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames#IMAGE}
 * configuration.
 */
@Component
class ImageConfigurator implements MessageConfigurator {
    @Override
    public void apply(BuilderWrapper builder, CommunicationMessage message, Map<String, String> characteristics) {
        builder.getNotificationBuilder().setImage(characteristics.get(ParameterNames.IMAGE));
    }
}
