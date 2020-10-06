package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config.InfobipViberConfig;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config.ViberTenantConfig;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config.ViberTenantConfigService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ViberConfigGetter {

    private final ViberTenantConfigService viberTenantConfigService;
    private final CacheKey cacheKey = new CacheKey();

    private LoadingCache<CacheKey, InfobipViberConfig> configCache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build(new CacheLoader<>() {
            @Override
            public InfobipViberConfig load(CacheKey key) {
                InfobipViberConfig.InfobipViberConfigBuilder builder = InfobipViberConfig.builder();

                ViberTenantConfig viberTenantConfig = viberTenantConfigService.getViberTenantConfig();

                builder.address(viberTenantConfig.getUrl());

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
        //to be specified later. Probably we'll need to get scenario key by sender from communication message so we'll add sender id to this class
    }

}
