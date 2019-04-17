package com.icthh.xm.tmf.ms.communication.rules;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component
public class BusinessRuleValidator {

   private final List<BusinessRule> businessRules;

    public BusinessRuleValidator(@Autowired(required = false) List<BusinessRule> businessRules) {
        this.businessRules = businessRules;
    }

    @PostConstruct
    public void init() {
        if (businessRules != null) {
            businessRules.sort(AnnotationAwareOrderComparator.INSTANCE);
        }
    }

    public void validate(CommunicationMessage message) {
        if (businessRules != null && businessRules.size() > 0) {
            businessRules.forEach(rule -> rule.validate(message));
        }
    }

}
