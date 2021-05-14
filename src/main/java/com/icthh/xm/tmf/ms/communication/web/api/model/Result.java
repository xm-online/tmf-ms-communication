package com.icthh.xm.tmf.ms.communication.web.api.model;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class Result {
    private Integer successCount;
    private Integer failureCount;
    private List<Detail> details;
}
