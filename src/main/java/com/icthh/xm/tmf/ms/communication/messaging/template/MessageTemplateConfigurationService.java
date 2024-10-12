package com.icthh.xm.tmf.ms.communication.messaging.template;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MessageTemplateConfigurationService implements RefreshableConfiguration {

    private final String pathPattern;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Map<String, String> msisdnTenantTemplates = new ConcurrentHashMap<>();

    public MessageTemplateConfigurationService(ApplicationProperties properties) {
        this.pathPattern = properties.getMsisdnPathPattern();
    }

    @Override
    public void onRefresh(String configPath, String config) {
        if (StringUtils.isBlank(config)) {
            msisdnTenantTemplates.remove(configPath);
            log.info("MSISDN template '{}' was removed", configPath);
        } else {
            msisdnTenantTemplates.put(configPath, config);
            log.info("MSISDN template '{}' was updated", configPath);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(pathPattern, updatedKey);
    }

    @Override
    public void onInit(String configPath, String config) {
        if (isListeningConfiguration(configPath)) {
            onRefresh(configPath, config);
        }
    }

    public String getMsisdnTemplateContent(String templatePath) {
        return Optional.ofNullable(msisdnTenantTemplates.get(templatePath))
            .orElseThrow(() -> new EntityNotFoundException(String.format("Msisdn template not found: %s", templatePath)));
    }
}
