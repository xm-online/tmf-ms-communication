package com.icthh.xm.tmf.ms.communication.domain;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.Data;

@Data
public class MessageResponse {

    private Status status;
    private String errorCode;
    private String errorMessage;
    private String messageId;

    private CommunicationMessageCreate responseTo;

    public enum Status {
        SUCCESS, FAILED;
    }

    public static MessageResponse success(String messageId, CommunicationMessageCreate responseTo) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.responseTo = responseTo;
        messageResponse.status = Status.SUCCESS;
        messageResponse.messageId = messageId;
        return messageResponse;
    }

    public static MessageResponse failed(CommunicationMessageCreate responseTo, Exception e) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.responseTo = responseTo;
        messageResponse.status = Status.FAILED;
        messageResponse.errorCode = e.getClass().getSimpleName();
        messageResponse.errorMessage = e.getMessage();
        return messageResponse;
    }

}
