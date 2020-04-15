package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.spring.SpringLepProcessingApplicationListener;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.*;


@RequiredArgsConstructor
@Service
public class XmCommunicationMsLepProcessingApplicationListener extends SpringLepProcessingApplicationListener {

    private final BusinessTimeConfigService tenantConfigService;
    private final RestTemplate restTemplate;
    private final CommonsService commonsService;
    private final PermissionCheckService permissionCheckService;
    private final KafkaTemplateService kafkaTemplateService;

    @Override
    protected void bindExecutionContext(ScopedContext executionContext) {
        // services
        Map<String, Object> services = new HashMap<>();
        services.put(BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE, tenantConfigService);
        services.put(BINDING_SUB_KEY_PERMISSION_SERVICE, permissionCheckService);
        executionContext.setValue(BINDING_KEY_SERVICES, services);

        executionContext.setValue(BINDING_KEY_COMMONS, new CommonsExecutor(commonsService));

        // templates
        Map<String, Object> templates = new HashMap<>();
        templates.put(BINDING_SUB_KEY_TEMPLATE_REST, restTemplate);
        templates.put(BINDING_SUB_KEY_TEMPLATE_KAFKA, kafkaTemplateService);

        executionContext.setValue(BINDING_KEY_TEMPLATES, templates);
    }
}

