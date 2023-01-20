package com.icthh.xm.tmf.ms.communication.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateTemplateRequest {
    @NotBlank
    private String content;
    @NotBlank
    private String subjectTemplate;
    @NotBlank
    private String emailFrom;
}
