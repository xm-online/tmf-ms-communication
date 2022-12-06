package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailTemplateSpec {
    private String templateKey;
    private String name;
    private String defaultSubjectTemplate;
    private String templatePath;
    private String defaultContext;
    private String contextSpec;
    private String contextForm;
}
