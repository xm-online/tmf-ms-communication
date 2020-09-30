package com.icthh.xm.tmf.ms.communication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.tmf.ms.communication.channel.telegram.TelegramChannelHandler;
import com.icthh.xm.tmf.ms.communication.channel.twilio.TwilioChannelHandler;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelRefreshableConfiguration implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private final ApplicationProperties properties;
    private final TelegramChannelHandler telegramChannelHandler;
    private final TwilioChannelHandler twilioChannelHandler;

    private AntPathMatcher matcher = new AntPathMatcher();
    private ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    @SneakyThrows
    @Override
    public void onRefresh(String updatedKey, String config) {
        refreshConfig(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(properties.getChannelSpecificationPathPattern(), updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        if (isListeningConfiguration(configKey)) {
            refreshConfig(configKey, configValue);
        }
    }

    private void refreshConfig(String updatedKey, String config) {
        if (StringUtils.isEmpty(config)) {
            return;
        }
        CommunicationSpec spec = readSpec(updatedKey, config);
        String tenantKey = extractTenant(updatedKey);

        telegramChannelHandler.onRefresh(tenantKey, spec);
        twilioChannelHandler.onRefresh(tenantKey, spec);
        //init other channels: sms, email, viber, etc
    }

    private CommunicationSpec readSpec(String updatedKey, String config) {
        CommunicationSpec spec = null;
        try {
            spec = ymlMapper.readValue(config, CommunicationSpec.class);
        } catch (Exception e) {
            log.error("Error read communication specification from path: {}", updatedKey, e);
        }
        return spec;
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(properties.getChannelSpecificationPathPattern(), updatedKey)
            .get(TENANT_NAME);
    }
}
