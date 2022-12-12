package com.icthh.xm.tmf.ms.communication.domain.dto;

import lombok.Data;

@Data
public class TemplateDetails {
    private String subject;
    private String name;
    private String content;
    private String emailSpec;
    private String emailForm;
    private String emailData;
}
