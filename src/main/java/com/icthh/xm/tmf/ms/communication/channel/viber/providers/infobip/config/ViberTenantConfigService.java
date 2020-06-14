package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Getter
public class ViberTenantConfigService extends TenantConfigService {

    private ViberTenantConfig viberTenantConfig;

    public ViberTenantConfigService(XmConfigProperties xmConfigProperties,
                                    TenantContextHolder tenantContextHolder) {
        super(xmConfigProperties, tenantContextHolder);
    }

    @SneakyThrows
    @Override
    public void onRefresh(String updatedKey, String config) {
        super.onRefresh(updatedKey, config);

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        viberTenantConfig = objectMapper.readValue(config, TenantConfig.class).getViber();
        log.debug("Update viber config: {}", viberTenantConfig);
    }

    @Data
    public static class TenantConfig {
        private ViberTenantConfig viber;
    }
}
