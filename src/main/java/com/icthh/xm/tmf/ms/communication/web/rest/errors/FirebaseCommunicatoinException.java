package com.icthh.xm.tmf.ms.communication.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class FirebaseCommunicatoinException extends AbstractThrowableProblem {

    public FirebaseCommunicatoinException(String message) {
        super(ErrorConstants.DEFAULT_TYPE, message, Status.INTERNAL_SERVER_ERROR);
    }
}
