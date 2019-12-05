package com.icthh.xm.tmf.ms.communication.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

@Slf4j
@UtilityClass
public class LogUtil {

    public static void withLog(String tenant, String command, Runnable action, String logTemplate, Object... params) {
        final StopWatch stopWatch = StopWatch.createStarted();
        log.info("[{}] start: {} " + logTemplate, tenant, command, params);
        action.run();
        log.info("[{}]  stop: {}, time = {} ms.", tenant, command, stopWatch.getTime());
    }
}
