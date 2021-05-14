package com.icthh.xm.tmf.ms.communication.web.api.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class Detail {
    private Status status;
    private Receiver receiver;
    private String messageId;
    private ErrorDetail error;

    public enum Status {
        SUCCESS,
        ERROR
    }
}
