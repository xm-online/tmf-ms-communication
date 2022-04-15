package com.icthh.xm.tmf.ms.communication.service.firebase;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.VALIDITY_PERIOD;
import static com.icthh.xm.tmf.ms.communication.utils.Utils.parseIntOrException;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Adds {@link com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames#VALIDITY_PERIOD}
 * configuration.
 */
@Component
@Slf4j
class ValidityPeriodConfigurator implements MessageConfigurator {
    @Override
    public void apply(BuilderWrapper builder, CommunicationMessage message, Map<String, String> characteristics) {
        String ttl = characteristics.get(VALIDITY_PERIOD);
        if (StringUtils.isBlank(ttl)) {
            return;
        }

        int ttlSeconds = parseIntOrException(ttl);

        log.debug("{} parameter is provided, adding to the request {}",
            VALIDITY_PERIOD, ttlSeconds);

        builder.getApnsBuilder().putHeader("apns-expiration",
            String.valueOf(Instant.now().plus(ttlSeconds, ChronoUnit.SECONDS).toEpochMilli() / 1000));

        builder.getAndroidConfigBuilder().setTtl(ttlSeconds * 1000L);

        builder.getWebPushBuilder().putHeader("ttl", String.valueOf(ttlSeconds));
    }
}
