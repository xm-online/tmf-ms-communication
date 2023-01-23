package com.icthh.xm.tmf.ms.communication.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public abstract class BaseTemplateDetails {
    private String contextSpec;
    private String contextForm;
    private String contextExample;
    private List<String> langs;
}
