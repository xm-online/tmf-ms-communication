package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class EmailTemplateSpec {
    private String templateKey;
    private String name;
    private Map<String,String> subjectTemplate;
    private Map<String, String> from;
    private String templatePath;
    private String contextExample;
    private String contextSpec;
    private String contextForm;

    public EmailTemplateSpec override(@Nullable CustomEmailTemplateSpec customEmailTemplateSpec) {
        Map<String, String> subject = Optional.ofNullable(customEmailTemplateSpec)
                .map(CustomEmailTemplateSpec::getSubjectTemplate)
                .orElse(subjectTemplate);
        Map<String, String> from = Optional.ofNullable(customEmailTemplateSpec)
            .map(CustomEmailTemplateSpec::getFrom)
            .orElse(this.from);

        return new EmailTemplateSpec(
                templateKey,
                name,
                subject,
                from,
                templatePath,
                contextExample,
                contextSpec,
                contextForm
        );
    }
}
