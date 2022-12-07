package com.icthh.xm.tmf.ms.communication.domain.spec;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerEmailSpec {
    private List<CustomerEmailTemplateSpec> emails;
}
