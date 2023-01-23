package com.icthh.xm.tmf.ms.communication.domain.dto;

import java.util.Map;
import lombok.Data;

@Data
public class TemplateMultiLangDetails extends BaseTemplateDetails {
    private Map<String, String> subjectTemplate;
    private Map<String, String> emailFrom;
    private Map<String, String> content;
}
