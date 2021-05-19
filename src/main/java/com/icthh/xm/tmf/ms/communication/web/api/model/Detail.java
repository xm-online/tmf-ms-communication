package com.icthh.xm.tmf.ms.communication.web.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class Detail {
    @JsonProperty("status")
    private Status status;
    @JsonProperty("receiver")
    private Receiver receiver;
    @JsonProperty("messageId")
    private String messageId;
    @JsonProperty("error")
    private ErrorDetail error;

    public enum Status {
        SUCCESS,
        ERROR
    }
}
