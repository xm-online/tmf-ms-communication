package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomerEmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.DefaultEmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpecificationType;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSpecificationService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, Map<String, EmailTemplateSpec>> emailSpecs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, EmailTemplateSpec>> customerEmailSpecs = new ConcurrentHashMap<>();
    private final ApplicationProperties properties;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final TenantContextHolder tenantContextHolder;

    @LoggingAspectConfig(resultDetails = false)
    public List<EmailTemplateSpec> getEmailSpecList() {
        String tenantKey = tenantContextHolder.getTenantKey();
        String cfgTenantKey = tenantKey.toUpperCase();
        if (!emailSpecs.containsKey(cfgTenantKey)) {
            throw new IllegalArgumentException("Email specification not found");
        }
        Map<String, EmailTemplateSpec> emailSpec = emailSpecs.getOrDefault(cfgTenantKey, emptyMap());
        Map<String, EmailTemplateSpec> customerEmailSpec = customerEmailSpecs.getOrDefault(cfgTenantKey, emptyMap());
        if (!customerEmailSpec.isEmpty()) {
            customerEmailSpec = new HashMap<>(customerEmailSpec);
        }
        customerEmailSpec.keySet().retainAll(emailSpec.keySet());
        Map<String, EmailTemplateSpec> processedEmailSpec = new HashMap<>(emailSpec);
        processedEmailSpec.putAll(customerEmailSpec);

        return new ArrayList<>(processedEmailSpec.values());
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            EmailSpecificationType emailSpecificationType = getEmailSpecificationType(updatedKey);
            String tenantName = getTenantKey(emailSpecificationType, updatedKey);
            Map<String, Map<String, EmailTemplateSpec>> updatedSpec = getUpdatedSpec(emailSpecificationType);

            if (StringUtils.isBlank(config)) {
                updatedSpec.remove(tenantName);
                log.info("Email specification for tenant {} was removed", tenantName);
            } else {
                List<EmailTemplateSpec> emailTemplateSpecs = parseEmailSpec(emailSpecificationType, config);
                Map<String, EmailTemplateSpec> emailSpecMap = emailTemplateSpecs.stream()
                    .collect(Collectors.toMap(EmailTemplateSpec::getTemplateKey, Function.identity()));
                updatedSpec.put(tenantName, emailSpecMap);
                log.info("Email specification for tenant {} was updated", tenantName);
            }
        } catch (Exception e) {
            log.error("Error read specification from path {}", updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(properties.getEmailSpecificationPathPattern(), updatedKey);
    }

    private EmailSpecificationType getEmailSpecificationType(String updatedKey) {
        if (matcher.match(properties.getEmailSpecificationPathPattern(), updatedKey)) {
            return EmailSpecificationType.DEFAULT;
        } else if (matcher.match(properties.getCustomEmailSpecificationPathPattern(), updatedKey)) {
            return EmailSpecificationType.CUSTOM;
        } else {
            return null;
        }
    }

    private Map<String, Map<String, EmailTemplateSpec>> getUpdatedSpec(EmailSpecificationType emailSpecificationType) {
        if (EmailSpecificationType.DEFAULT.equals(emailSpecificationType)) {
            return emailSpecs;
        } else if (EmailSpecificationType.CUSTOM.equals(emailSpecificationType)) {
            return customerEmailSpecs;
        } else {
            return emptyMap();
        }
    }

    private String getTenantKey(EmailSpecificationType emailSpecificationType, String updatedKey) {
        String emailSpecPattern;
        if (EmailSpecificationType.DEFAULT.equals(emailSpecificationType)) {
            emailSpecPattern = properties.getEmailSpecificationPathPattern();
        } else if (EmailSpecificationType.CUSTOM.equals(emailSpecificationType)) {
            emailSpecPattern = properties.getCustomEmailSpecificationPathPattern();
        } else {
            throw new IllegalArgumentException("Error read specification from path " + updatedKey);
        }
        return matcher.extractUriTemplateVariables(emailSpecPattern, updatedKey).get(TENANT_NAME);
    }

    @SneakyThrows
    private List<EmailTemplateSpec> parseEmailSpec(EmailSpecificationType emailSpecificationType, String config) {
        Map<String, List<EmailTemplateSpec>> emailSpec;
        if (EmailSpecificationType.DEFAULT.equals(emailSpecificationType)) {
            emailSpec = mapper.readValue(config, new TypeReference<Map<String, List<DefaultEmailTemplateSpec>>>(){});
        } else if (EmailSpecificationType.CUSTOM.equals(emailSpecificationType)) {
            emailSpec = mapper.readValue(config, new TypeReference<Map<String, List<CustomerEmailTemplateSpec>>>(){});
        } else {
            return emptyList();
        }
        return emailSpec.get("emails");
    }
}
