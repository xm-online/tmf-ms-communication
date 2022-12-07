package com.icthh.xm.tmf.ms.communication.domain.spec;

import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode()
@ToString(callSuper = true)
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

    public EmailTemplateSpec override(@Nullable CustomerEmailTemplateSpec customerEmailTemplateSpec) {
        String subject = Optional.ofNullable(customerEmailTemplateSpec)
                .map(CustomerEmailTemplateSpec::getSubjectTemplate)
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
