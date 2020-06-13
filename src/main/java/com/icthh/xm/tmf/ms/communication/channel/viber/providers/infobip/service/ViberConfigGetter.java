package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config.InfobipViberConfig;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ViberConfigGetter {

    private final BusinessTimeConfigService tenantConfigService;
    private final CacheKey cacheKey = new CacheKey();

    private LoadingCache<CacheKey, InfobipViberConfig> configCache = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .build(new CacheLoader<>() {
            @Override
            public InfobipViberConfig load(CacheKey key) {
                InfobipViberConfig.InfobipViberConfigBuilder builder = InfobipViberConfig.builder();

                Map<String, Object> config = tenantConfigService.getConfig();
                Map<String, String> viberConfig = (Map<String, String>) config.get("viber");

                builder.address(viberConfig.get("url"));
                builder.token(viberConfig.get("token"));
                builder.scenarioKey(viberConfig.get("scenario"));

                return builder.build();
            }
        });

    public InfobipViberConfig getForMessage(CommunicationMessage communicationMessage) {
        return configCache.getUnchecked(cacheKey);
    }

    public InfobipViberConfig getCommon() {
        return configCache.getUnchecked(cacheKey);
    }

    @Data
    @Builder
    static class CacheKey {
        //to be specified later. Probably we'll need to get scenario key by sender from communication message
    }

}
