package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

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
        String subject = getCustomEmailTemplateField(customEmailTemplateSpec, CustomEmailTemplateSpec::getSubjectTemplate, subjectTemplate);
        String templateName = getCustomEmailTemplateField(customEmailTemplateSpec, CustomEmailTemplateSpec::getName, name);

        return new EmailTemplateSpec(
                templateKey,
                templateName,
                subject,
                templatePath,
                contextExample,
                contextSpec,
                contextForm
        );
    }

    private String getCustomEmailTemplateField(CustomEmailTemplateSpec customEmailTemplateSpec,
                                               Function<CustomEmailTemplateSpec, String> getter,
                                               String defaultValue) {
       return Optional.ofNullable(customEmailTemplateSpec)
            .map(getter)
            .orElse(defaultValue);
    }
}
