package com.icthh.xm.tmf.ms.communication.web.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ErrorDetail {
    @JsonProperty("code")
    private String code;
    @JsonProperty("description")
    private String description;
}
