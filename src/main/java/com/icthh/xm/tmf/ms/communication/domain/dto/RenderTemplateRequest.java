package com.icthh.xm.tmf.ms.communication.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class RenderTemplateRequest {
    @NotBlank
    private String content;
    @NotNull
    private Map<String, Object> model;
    private String templatePath;
    private String lang;
}
