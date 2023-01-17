package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

@Component
@RequiredArgsConstructor
public class ExecuteTenantContextUtils {

    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;
    private final LepManager lepManager;

    public void runInTenantContext(String tenant, Runnable operation) {
        tenantContextHolder.getPrivilegedContext().execute(buildTenant(tenant.toUpperCase()), () -> {
            try {
                init();
                operation.run();
            } finally {
                destroy();
            }
        });
    }

    private void init() {
        lepManager.beginThreadContext(threadContext -> {
            threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    private void destroy() {
        lepManager.endThreadContext();
    }
}
