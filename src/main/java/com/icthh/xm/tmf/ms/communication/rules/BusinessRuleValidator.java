package com.icthh.xm.tmf.ms.communication.rules;

import static java.util.Collections.emptyList;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BusinessRuleValidator {

   private final List<BusinessRule> businessRules;

    public BusinessRuleValidator(@Autowired(required = false) List<BusinessRule> businessRules) {
        this.businessRules = businessRules;
    }

    public List<String> validate(CommunicationMessage message) {
        if (businessRules != null && businessRules.size() > 0) {
            List<String> result = new ArrayList<>();
            businessRules.forEach(rule -> result.add(rule.validate(message)));
            return result;
        }
        return emptyList();
    }

}
