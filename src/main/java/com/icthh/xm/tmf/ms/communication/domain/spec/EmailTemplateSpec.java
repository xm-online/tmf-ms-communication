package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class EmailTemplateSpec implements Comparable<EmailTemplateSpec>{
    private String templateKey;
    private String name;
    private String defaultSubjectTemplate;
    private String templatePath;
    private String defaultContext;
    private String contextSpec;
    private String contextForm;

    @Override
    public int compareTo(EmailTemplateSpec o) {
        return templateKey.compareTo(o.getTemplateKey());
    }
}
