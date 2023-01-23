package com.icthh.xm.tmf.ms.communication.domain.dto;

import lombok.Data;

@Data
public class TemplateDetails extends BaseTemplateDetails {
    private String subjectTemplate;
    private String emailFrom;
    private String content;
    private String templatePath;
}
