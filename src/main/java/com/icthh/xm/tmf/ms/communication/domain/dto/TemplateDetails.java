package com.icthh.xm.tmf.ms.communication.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateDetails extends BaseTemplateDetails {
    private String subjectTemplate;
    private String emailFrom;
    private String content;
}
