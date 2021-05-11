package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.Map;

/**
 * Describes a component that is able to configure Firebase message, e.g. apply
 * parameters based on the request.
 */
interface MessageConfigurator {

    /**
     * Applies a configuration
     *
     * @param builder         message builder wrapper
     * @param message         message request
     * @param characteristics request characteristics
     */
    void apply(BuilderWrapper builder, CommunicationMessage message,
               Map<String, String> characteristics);
}
