package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import javax.annotation.Nullable;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class EmailTemplateSpec {
    private String templateKey;
    private String name;
    private String subjectTemplate;
    private String templatePath;
    private String contextExample;
    private String contextSpec;
    private String contextForm;

    public EmailTemplateSpec override(@Nullable CustomEmailTemplateSpec customEmailTemplateSpec) {
        String subject = Optional.ofNullable(customEmailTemplateSpec)
                .map(CustomEmailTemplateSpec::getSubjectTemplate)
                .orElse(subjectTemplate);

        return new EmailTemplateSpec(
                templateKey,
                name,
                subject,
                templatePath,
                contextExample,
                contextSpec,
                contextForm
        );
    }
}
