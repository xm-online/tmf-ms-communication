package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

@Component
@RequiredArgsConstructor
public class ExecuteTenantContextUtils {

    private final TenantContextHolder tenantContextHolder;
    private final LepManagementService lepManagementService;

    public void runInTenantContext(String tenant, Runnable operation) {
        tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant.toUpperCase()), () -> {
            try (var context = lepManagementService.beginThreadContext()) {
                operation.run();
            }
        });
    }
}
