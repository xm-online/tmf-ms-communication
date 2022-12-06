package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class EmailTemplateSpec implements Comparable<EmailTemplateSpec> {
    private String templateKey;

    @Override
    public int compareTo(EmailTemplateSpec o) {
        return templateKey.compareTo(o.getTemplateKey());
    }
}
