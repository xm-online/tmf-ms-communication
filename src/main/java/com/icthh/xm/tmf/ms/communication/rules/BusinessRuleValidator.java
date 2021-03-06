package com.icthh.xm.tmf.ms.communication.rules;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BusinessRuleValidator {

    private final List<BusinessRule> businessRules;

    public BusinessRuleValidator(@Autowired(required = false) List<BusinessRule> businessRules) {
        this.businessRules = businessRules;
    }

    public RuleResponse validate(CommunicationMessage message) {
        if (businessRules == null) {
            return new RuleResponse();
        }
        for (BusinessRule businessRule : businessRules) {
            RuleResponse response = businessRule.validate(message);
            if (!response.isSuccess()) {
                return response;
            }
        }  
        return new RuleResponse();
    }
}
