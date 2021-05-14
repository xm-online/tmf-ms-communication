package com.icthh.xm.tmf.ms.communication.web.api.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ErrorDetail {
    private String code;
    private String description;
}
