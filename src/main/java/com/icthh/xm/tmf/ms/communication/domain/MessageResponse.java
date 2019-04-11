package com.icthh.xm.tmf.ms.communication.domain;

import static com.google.common.base.Predicates.not;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.FAILED;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.SUCCESS;
import static java.util.Collections.emptyList;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Data;

@Data
public class MessageResponse {

    public static final String DISTRIBUTION_ID = "DISTRIBUTION.ID";

    private Status status;
    private String errorCode;
    private String errorMessage;
    @Nullable
    private String messageId; //smsc id
    private String distributionId; //entity.id
    private String id; //entity.id-chanel-recipient.id

    private CommunicationMessage responseTo;

    public MessageResponse(Status status, CommunicationMessage responseTo) {
        this.status = status;
        this.responseTo = responseTo;
        String distributionId = getDistributionId(responseTo);
        this.distributionId = distributionId;
        this.id = buildId(responseTo, distributionId);
    }

    public static MessageResponse success(String messageId, CommunicationMessage responseTo) {
        MessageResponse messageResponse = new MessageResponse(SUCCESS, responseTo);
        messageResponse.messageId = messageId;
        return messageResponse;
    }

    public static String buildId(CommunicationMessage responseTo, String distributionId) {
        return distributionId + "-" + responseTo.getType() + "-" + getFirstReceiverId(responseTo);
    }

    public static MessageResponse failed(CommunicationMessage responseTo, Exception e) {
        MessageResponse messageResponse = new MessageResponse(FAILED, responseTo);
        messageResponse.errorCode = e.getClass().getSimpleName();
        messageResponse.errorMessage = e.getMessage();
        return messageResponse;
    }

    private static String getDistributionId(CommunicationMessage responseTo) {
        return nullSafe(responseTo).stream()
                                   .filter(it -> it.getName().equals(DISTRIBUTION_ID))
                                   .findFirst()
                                   .map(CommunicationRequestCharacteristic::getValue)
                                   .orElse(UUID.randomUUID().toString());
    }

    private static List<CommunicationRequestCharacteristic> nullSafe(CommunicationMessage responseTo) {
        return responseTo.getCharacteristic() == null ? emptyList() : responseTo.getCharacteristic();
    }

    private static String getFirstReceiverId(CommunicationMessage responseTo) {
        return Optional.ofNullable(responseTo.getReceiver())
                       .filter(not(List::isEmpty))
                       .map(list -> list.get(0))
                       .map(Receiver::getId)
                       .orElse(null);
    }

    public enum Status {
        SUCCESS, FAILED;
    }

}
