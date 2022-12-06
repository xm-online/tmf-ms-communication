package com.icthh.xm.tmf.ms.communication.web.rest.errors;

import lombok.Getter;
import org.zalando.problem.AbstractThrowableProblem;

import java.util.Map;

@Getter
public class RenderTemplateException extends AbstractThrowableProblem {

    private final String message;
    private final String content;
    private final Map model;

    public RenderTemplateException(String message, String content, Map model) {
        this.message = message;
        this.content = content;
        this.model = model;
    }
}
