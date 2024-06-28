package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.processor.GroovyMap;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.client.RestTemplate;

@GroovyMap
public class LepContext extends BaseLepContext  {

    public LepServices services;
    public LepTemplates templates;
    public CommonsExecutor commonsService;
    public MeterRegistry meterRegistry;


    public static class LepServices {
        public MailService mailService;
        public BusinessTimeConfigService tenantConfigService;
        public PermissionCheckService permissionService;
    }

    public static class LepTemplates {
        public RestTemplate rest;
        public RestTemplate loadBalancedRest;
        public KafkaTemplateService kafka;
    }
}
