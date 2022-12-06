package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class DefaultEmailTemplateSpec extends EmailTemplateSpec {
    private String name;
    private String defaultSubjectTemplate;
    private String templatePath;
    private String contextExample;
    private String contextSpec;
    private String contextForm;
}
