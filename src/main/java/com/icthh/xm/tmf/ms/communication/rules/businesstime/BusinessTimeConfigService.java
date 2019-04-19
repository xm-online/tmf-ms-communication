package com.icthh.xm.tmf.ms.communication.rules.businesstime;

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
public class BusinessTimeConfigService extends TenantConfigService {

    private BusinessDayConfig businessDayConfig;
    private final ObjectMapper objectMapper;

    public BusinessTimeConfigService(XmConfigProperties xmConfigProperties,
        TenantContextHolder tenantContextHolder, ObjectMapper objectMapper) {
        super(xmConfigProperties, tenantContextHolder);
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public void onRefresh(String updatedKey, String config) {
        super.onRefresh(updatedKey, config);

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        businessDayConfig = objectMapper.readValue(config, BusinessDayConfig.class);
        log.debug("Update business day config: {}", businessDayConfig);
    }

}
