package com.icthh.xm.tmf.ms.communication.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateDetails {
    private String subjectTemplate;
    private String from;
    private String content;
    private String contextSpec;
    private String contextForm;
    private String contextExample;
    private List<String> langs;
}
