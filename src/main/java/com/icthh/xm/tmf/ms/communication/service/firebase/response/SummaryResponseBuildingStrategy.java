package com.icthh.xm.tmf.ms.communication.service.firebase.response;

import com.google.firebase.messaging.BatchResponse;
import com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames;
import com.icthh.xm.tmf.ms.communication.service.firebase.ExtendedCommunicationMessageFactory;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.ExtendedCommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Result;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Includes success and error counts only.
 */
@Component
public class SummaryResponseBuildingStrategy implements ResponseBuildingStrategy {

    @Override
    public String getName() {
        return ParameterNames.ResultType.SUMMARY.name();
    }

    @Override
    public ExtendedCommunicationMessage buildCommunicationResponse(BatchResponse batchResponse,
                                                                   CommunicationMessage msg, List<Receiver> receivers) {
        ExtendedCommunicationMessage message = ExtendedCommunicationMessageFactory.newMessage(msg);
        message.id(UUID.randomUUID().toString());
        message.result(new Result()
            .successCount(batchResponse.getSuccessCount())
            .failureCount(batchResponse.getFailureCount()));
        return message;
    }


}
