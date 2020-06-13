package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config.InfobipViberConfig;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ViberConfigGetter implements Function<CommunicationMessage, InfobipViberConfig> {

    private final BusinessTimeConfigService tenantConfigService;

    private LoadingCache<CommunicationMessage, InfobipViberConfig> configCache = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .build(new CacheLoader<>() {
            @Override
            public InfobipViberConfig load(CommunicationMessage key) {
                InfobipViberConfig.InfobipViberConfigBuilder builder = InfobipViberConfig.builder();

                Map<String, Object> config = tenantConfigService.getConfig();
                Map<String, String> viberConfig = (Map<String, String>) config.get("viber");

                builder.address(viberConfig.get("url"));
                builder.token(viberConfig.get("token"));
                builder.scenarioKey(viberConfig.get("scenario"));

                return builder.build();
            }
        });

    @Override
    public InfobipViberConfig apply(CommunicationMessage communicationMessage) {
        return configCache.getUnchecked(communicationMessage);
    }

}
