package com.icthh.xm.tmf.ms.communication.domain.spec;

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
public class CustomerEmailTemplateSpec {
    private String templateKey;
    private String subjectTemplate;
}
