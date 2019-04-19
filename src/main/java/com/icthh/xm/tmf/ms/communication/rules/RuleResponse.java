package com.icthh.xm.tmf.ms.communication.rules;

import java.util.Map;
import lombok.Data;


@Data
public class RuleResponse {

    private String ruleType;
    private boolean success = true;
    private String responseCode;
    Map<String, Object> context;
}
