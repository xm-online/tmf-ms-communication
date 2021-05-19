package com.icthh.xm.tmf.ms.communication.web.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class Result {
    @JsonProperty("successCount")
    private Integer successCount;
    @JsonProperty("failureCount")
    private Integer failureCount;
    @JsonProperty("details")
    private List<Detail> details;
}
