package com.icthh.xm.tmf.ms.communication.web.rest.errors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zalando.problem.AbstractThrowableProblem;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class RenderTemplateException extends AbstractThrowableProblem {
    private final String message;
    private final String content;
    private final Map model;
}
