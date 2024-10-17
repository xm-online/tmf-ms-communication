package com.icthh.xm.tmf.ms.communication.messaging.template;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
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

    private final String twilioPathPattern;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Map<String, String> twilioTenantTemplates = new ConcurrentHashMap<>();

    public MessageTemplateConfigurationService(ApplicationProperties properties) {
        this.twilioPathPattern = properties.getTwilioPathPattern();
    }

    @Override
    public void onRefresh(String configPath, String config) {
        if (matcher.match(twilioPathPattern, configPath)) {
            refreshMessageTemplateConfiguration(configPath, config, twilioTenantTemplates);
        }
    }

    private void refreshMessageTemplateConfiguration(String configPath, String config, Map<String, String> tenantTemplates) {
        if (StringUtils.isBlank(config)) {
            tenantTemplates.remove(configPath);
            log.info("Message template '{}' was removed", configPath);
        } else {
            tenantTemplates.put(configPath, config);
            log.info("Message template '{}' was updated", configPath);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(twilioPathPattern, updatedKey);
    }

    @Override
    public void onInit(String configPath, String config) {
        if (isListeningConfiguration(configPath)) {
            onRefresh(configPath, config);
        }
    }

    public String getTemplateContent(String templatePath, MessageType messageType) {
        switch (messageType) {
            case Twilio: return getTemplateContent(templatePath, twilioTenantTemplates);
            default: throw new EntityNotFoundException(String.format("Message template not found: %s", templatePath));
        }
    }

    public String getTemplateContent(String templatePath, Map<String, String> templatesMap) {
        return Optional.ofNullable(templatesMap.get(templatePath))
            .orElseThrow(() -> new EntityNotFoundException(String.format("Template not found: %s", templatePath)));
    }
}
