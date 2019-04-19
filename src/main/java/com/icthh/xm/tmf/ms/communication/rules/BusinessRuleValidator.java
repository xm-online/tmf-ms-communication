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
        if (businessRules != null && businessRules.size() > 0) {
            for (BusinessRule businessRule : businessRules) {
                RuleResponse validate = businessRule.validate(message);
                if (!validate.isSuccess()) {
                    return validate;
                }
            }
        }
        RuleResponse ruleResponse = new RuleResponse();
        ruleResponse.setSuccess(true);
        return ruleResponse;
    }
}
