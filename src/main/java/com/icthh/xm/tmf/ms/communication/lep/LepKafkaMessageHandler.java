package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LepKafkaMessageHandler {

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;

    public void preHandler(String tenantKey) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        lepManagementService.beginThreadContext();
    }

    public void destroy() {
        TenantContextUtils.getTenantKey(tenantContextHolder).ifPresent(t -> {
            lepManagementService.endThreadContext();
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        });
    }
}
