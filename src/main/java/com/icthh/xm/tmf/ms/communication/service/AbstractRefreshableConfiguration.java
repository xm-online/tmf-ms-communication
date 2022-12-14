package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRefreshableConfiguration<T> implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, T> configurations = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenantName = matcher.extractUriTemplateVariables(getConfigPathPattern(), updatedKey).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                configurations.remove(tenantName);
                log.info("Specification by path {} for tenant {} was removed", updatedKey, tenantName);
            } else {
                T configuration = mapper.readValue(config, getConfigClass());
                configurations.put(tenantName, configuration);
                log.info("Specification by path {} for tenant {} was updated", updatedKey, tenantName);
            }
        } catch (Exception e) {
            log.error("Error read specification from path {}", updatedKey, e);
        }
    }

    protected Map<String, T> getConfigurations() {
        return Collections.unmodifiableMap(configurations);
    }

    public Optional<T> getConfiguration(String tenantKey) {
        return Optional.ofNullable(configurations.get(tenantKey));
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(getConfigPathPattern(), updatedKey);
    }

    public abstract String getConfigPathPattern();

    public abstract Class<T> getConfigClass();

}
