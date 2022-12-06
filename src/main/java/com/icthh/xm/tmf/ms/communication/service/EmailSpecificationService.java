package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSpecificationService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, Map<String, EmailTemplateSpec>> emailSpecs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, EmailTemplateSpec>> customerEmailSpecs = new ConcurrentHashMap<>();
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
        Map<String, EmailTemplateSpec> emailSpec = emailSpecs.get(cfgTenantKey);
        Map<String, EmailTemplateSpec> customerEmailSpec = customerEmailSpecs.get(cfgTenantKey);
        Map<String, EmailTemplateSpec> processedEmailSpec = new HashMap<>(emailSpec);
        processedEmailSpec.putAll(customerEmailSpec);

        return new ArrayList<>(processedEmailSpec.values());
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenantName = matcher.extractUriTemplateVariables(properties.getEmailSpecificationPathPattern(), updatedKey).get(TENANT_NAME);

            if (StringUtils.isBlank(config)) {
                emailSpecs.remove(tenantName);
                log.info("Email specification for tenant {} was removed", tenantName);
            } else {
                EmailSpec emailSpec = mapper.readValue(config, EmailSpec.class);
                List<EmailTemplateSpec> emailTemplateSpecs = emailSpec.getEmailTemplateSpecs();
                Map<String, EmailTemplateSpec> emailSpecMap = emailTemplateSpecs.stream()
                    .collect(Collectors.toMap(EmailTemplateSpec::getTemplateKey, Function.identity()));
                emailSpecs.put(tenantName, emailSpecMap);
                log.info("Email specification for tenant {} was updated", tenantName);
            }
        } catch (Exception e) {
            log.error("Error read specification from path {}" + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(properties.getEmailSpecificationPathPattern(), updatedKey);
    }
}
