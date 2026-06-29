package com.icthh.xm.tmf.ms.communication;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.tmf.ms.communication.lep.LepContext;
import com.icthh.xm.tmf.ms.communication.service.lep.DynamicTestLepService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan("com.icthh.xm.tmf.ms.communication.service.lep")
public class LepTestConfiguration extends com.icthh.xm.tmf.ms.communication.TestConfiguration {

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.XM_MS_CONFIG;
    }

    @Bean
    public DynamicTestLepService testLepService() {
        return new DynamicTestLepService();
    }

    @Bean
    public LepContextFactory lepContextFactory() {
        return lepMethod -> new LepContext();
    }
}
