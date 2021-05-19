package com.icthh.xm.tmf.ms.communication.web.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public class ExtendedCommunicationMessage extends CommunicationMessage {
    @JsonProperty("result")
    private Result result;
}
