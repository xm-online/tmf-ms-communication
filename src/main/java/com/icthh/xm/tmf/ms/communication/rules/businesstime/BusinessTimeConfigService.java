package com.icthh.xm.tmf.ms.communication.rules.businesstime;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Slf4j
@Component
@Primary
@Getter
public class BusinessTimeConfigService extends TenantConfigService {

    private BusinessDayConfig businessDayConfig;

    public BusinessTimeConfigService(XmConfigProperties xmConfigProperties,
        TenantContextHolder tenantContextHolder) {
        super(xmConfigProperties, tenantContextHolder);
    }

    @SneakyThrows
    @Override
    public void onRefresh(String updatedKey, String config) {
        super.onRefresh(updatedKey, config);

        ObjectMapper objectMapper = YAMLMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true)
                .build();

        businessDayConfig = objectMapper.readValue(config, BusinessDayConfig.class);
        log.debug("Update business day config: {}", businessDayConfig);
    }

}
