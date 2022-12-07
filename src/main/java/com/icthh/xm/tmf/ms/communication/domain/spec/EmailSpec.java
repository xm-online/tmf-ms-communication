package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailSpec {
    private List<EmailTemplateSpec> emails;

    public EmailSpec override(@Nullable CustomerEmailSpec emailSpec) {
        if (emailSpec == null) {
            return new EmailSpec(emails);
        }

        Map<String, CustomerEmailTemplateSpec> customerEmails = emailSpec.getEmails().stream()
                .collect(toMap(CustomerEmailTemplateSpec::getTemplateKey, identity()));
        var emails = this.emails.stream().map(it -> it.override(customerEmails.get(it.getTemplateKey()))).collect(toList());
        return new EmailSpec(emails);
    }
}
