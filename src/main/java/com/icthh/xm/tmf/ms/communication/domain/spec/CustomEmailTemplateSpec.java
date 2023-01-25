package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CustomEmailTemplateSpec {
    private String templateKey;
    private Map<String, String> subjectTemplate;
    private Map<String, String> emailFrom;

    public void updateSubjectTemplate(Map<String, String> subjectTemplate) {
        if (this.subjectTemplate == null) {
            this.subjectTemplate = subjectTemplate;
        }
        this.subjectTemplate.putAll(firstNonNull(subjectTemplate, Map.of()));
    }

    public void updateEmailFrom(Map<String, String> emailFrom) {
        if (this.emailFrom == null) {
            this.emailFrom = emailFrom;
        }
        this.emailFrom.putAll(firstNonNull(emailFrom, Map.of()));
    }
}
