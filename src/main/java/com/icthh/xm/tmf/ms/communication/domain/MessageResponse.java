package com.icthh.xm.tmf.ms.communication.domain;

import static com.google.common.base.Predicates.not;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Data;

@Data
public class MessageResponse {

    private Status status;
    private String errorCode;
    private String errorMessage;
    @Nullable
    private String messageId; //smsc id
    private String distributionId; //entity.id
    private String id; //entity.id-chanel-recipient.id

    private CommunicationMessage responseTo;

    public enum Status {
        SUCCESS, FAILED;
    }

    public static MessageResponse success(String messageId, CommunicationMessage responseTo) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.responseTo = responseTo;
        messageResponse.status = Status.SUCCESS;
        messageResponse.messageId = messageId;
        messageResponse.distributionId =
        messageResponse.id = messageResponse.distributionId + "-" +  responseTo.getType() + "-" + getFirstReceiverId(responseTo);
        return messageResponse;
    }

    private static String getFirstReceiverId(CommunicationMessage responseTo) {
        return Optional.ofNullable(responseTo.getReceiver())
                       .filter(not(List::isEmpty))
                       .map(list -> list.get(0))
                       .map(Receiver::getId)
                       .orElse(null);
    }

    public static MessageResponse failed(CommunicationMessage responseTo, Exception e) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.responseTo = responseTo;
        messageResponse.status = Status.FAILED;
        messageResponse.errorCode = e.getClass().getSimpleName();
        messageResponse.errorMessage = e.getMessage();
        return messageResponse;
    }
}
