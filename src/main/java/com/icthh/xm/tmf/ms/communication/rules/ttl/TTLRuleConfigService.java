package com.icthh.xm.tmf.ms.communication.rules.ttl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@Getter
public class TTLRuleConfigService extends TenantConfigService {

    TTLRuleConfig ttlRuleConfig;

    public TTLRuleConfigService(XmConfigProperties xmConfigProperties, TenantContextHolder tenantContextHolder) {
        super(xmConfigProperties, tenantContextHolder);
    }

    @SneakyThrows
    @Override
    public void onRefresh(String updatedKey, String config) {
        super.onRefresh(updatedKey, config);

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ttlRuleConfig = objectMapper.readValue(config, TTLRuleConfig.class);
        if (ttlRuleConfig != null && ttlRuleConfig.getAction() == null){
            log.warn("TTLRule action is not set, {} value will be used as default!", TTLRuleConfig.Action.getDefaultValue());
        }
        log.debug("Update ttlRuleConfig: {}", ttlRuleConfig);
    }

}
