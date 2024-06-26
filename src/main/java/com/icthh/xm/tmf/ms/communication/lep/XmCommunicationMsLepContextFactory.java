package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class XmCommunicationMsLepContextFactory implements LepContextFactory {

    private final BusinessTimeConfigService tenantConfigService;
    private final RestTemplate restTemplate;
    private final RestTemplate loadBalancedRestTemplate;
    private final CommonsService commonsService;
    private final PermissionCheckService permissionCheckService;
    private final KafkaTemplateService kafkaTemplateService;
    private final MailService mailService;
    private final MeterRegistry meterRegistry;

    @Override
    public BaseLepContext buildLepContext(LepMethod lepMethod) {
        LepContext lepContext = new LepContext();
        lepContext.services = new LepContext.LepServices();
        lepContext.services.tenantConfigService = tenantConfigService;
        lepContext.services.permissionService = permissionCheckService;
        lepContext.services.mailService = mailService;
        lepContext.services.commonsService = new CommonsExecutor(commonsService);
        lepContext.services.meterRegistry = meterRegistry;

        lepContext.templates = new LepContext.LepTemplates();
        lepContext.templates.kafka = kafkaTemplateService;
        lepContext.templates.loadBalancedRest = loadBalancedRestTemplate;
        lepContext.templates.rest = restTemplate;

        return lepContext;
    }
}
