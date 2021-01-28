package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RuleValidationException extends RuntimeException {
    @Getter
    private final RuleResponse ruleResponse;
}
