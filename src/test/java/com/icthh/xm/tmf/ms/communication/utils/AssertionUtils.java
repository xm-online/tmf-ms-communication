package com.icthh.xm.tmf.ms.communication.utils;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AssertionUtils {

    public static void tryAssertionUntilTimeout(Runnable assertions, int count, TimeUnit unit) throws Exception, AssertionError {
        long deadlineTimeMs = new Date().getTime() + unit.toMillis(count);

        AssertionError lastAssertionError = null;
        while (new Date().getTime() < deadlineTimeMs) {
            try {
                assertions.run();
                return;
            } catch (AssertionError ae) {
                lastAssertionError = ae;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        throw Objects.requireNonNull(lastAssertionError);
    }
}
