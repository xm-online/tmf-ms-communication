package com.icthh.xm.tmf.ms.communication.service.firebase.response;

import com.google.firebase.messaging.BatchResponse;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.ExtendedCommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;

/**
 * Describes a strategy of building a message response.
 */
public interface ResponseBuildingStrategy {

    /**
     * Builds a message response
     */
    ExtendedCommunicationMessage buildCommunicationResponse(BatchResponse batchResponse,
                                                            CommunicationMessage msg, List<Receiver> receivers);

    /**
     * @return strategy name
     */
    String getName();
}
