package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.tmf.ms.communication.lep.fields.CommonsField;
import com.icthh.xm.tmf.ms.communication.lep.fields.MeterRegistryField;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.client.RestTemplate;

public class LepContext extends BaseLepContext implements MeterRegistryField, CommonsField {

    public LepServices services;
    public LepTemplates templates;

    public static class LepServices {
        public MailService mailService;
        public MeterRegistry meterRegistry;
        public BusinessTimeConfigService tenantConfigService;
        public PermissionCheckService permissionService;
        public CommonsExecutor commonsService;
    }

    public static class LepTemplates {
        public RestTemplate rest;
        public RestTemplate loadBalancedRest;
        public KafkaTemplateService kafka;
    }
}
