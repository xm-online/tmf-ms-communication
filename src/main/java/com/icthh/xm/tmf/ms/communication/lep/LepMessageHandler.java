package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.commons.topic.message.MessageService;
import com.icthh.xm.lep.api.LepManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

@Slf4j
@Component
@RequiredArgsConstructor
public class LepMessageHandler {

    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;
    private final LepManager lepManager;

    public void preHandler(String tenantKey) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        lepManager.beginThreadContext(threadContext -> {
            threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    public void destroy() {
        TenantContextUtils.getTenantKey(tenantContextHolder).ifPresent(t -> {
            lepManager.endThreadContext();
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        });
    }
}
