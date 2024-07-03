package com.icthh.xm.lep

import com.icthh.xm.commons.config.client.service.TenantAliasService
import com.icthh.xm.commons.lep.TenantScriptStorage
import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration
import com.icthh.xm.commons.lep.spring.LepUpdateMode
import com.icthh.xm.commons.logging.config.LoggingConfigService
import com.icthh.xm.commons.logging.config.LoggingConfigServiceStub
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

/**
 * All LEP related configuration should have @Profile('leptest') annotation to prevent interfering with main java test.
 */
@TestConfiguration
@Profile('leptest')
class LepTestConfiguration extends GroovyLepEngineConfiguration {

    LepTestConfiguration() {
        super("testApp")
    }

    @Override
    protected TenantScriptStorage getTenantScriptStorageType() {
        return TenantScriptStorage.CLASSPATH
    }

    @Override
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }

    @Bean
    public TenantAliasService tenantAliasService() {
        return new TenantAliasService(mock(CommonConfigRepository.class), mock(TenantListRepository.class));
    }
}
