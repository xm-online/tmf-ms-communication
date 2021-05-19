package com.icthh.xm.tmf.ms.communication.service.firebase.response;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.SendResponse;
import com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Detail;
import com.icthh.xm.tmf.ms.communication.web.api.model.ErrorDetail;
import com.icthh.xm.tmf.ms.communication.web.api.model.ExtendedCommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Includes success and error counts, detailed description of error cases.
 */
@Component
public class ErrorOnlyResponseBuildingStrategy extends SummaryResponseBuildingStrategy {

    @Override
    public String getName() {
        return ParameterNames.ResultType.ERROR.name();
    }

    @Override
    public ExtendedCommunicationMessage buildCommunicationResponse(BatchResponse batchResponse,
                                                                   CommunicationMessage msg, List<Receiver> receivers) {

        ExtendedCommunicationMessage message = super.buildCommunicationResponse(batchResponse, msg, receivers);

        List<Detail> details = new ArrayList<>();
        List<SendResponse> responses = batchResponse.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);

            if (!response.isSuccessful()) {
                FirebaseMessagingException exception = response.getException();

                details.add(new Detail()
                    .receiver(receivers.get(i))
                    .status(Detail.Status.ERROR)
                    .error(new ErrorDetail()
                        .code(String.valueOf(exception.getMessagingErrorCode()))
                        .description(exception.getMessage())));
            }
        }

        message.result().details(details);
        message.id(UUID.randomUUID().toString());

        return message;
    }
}
