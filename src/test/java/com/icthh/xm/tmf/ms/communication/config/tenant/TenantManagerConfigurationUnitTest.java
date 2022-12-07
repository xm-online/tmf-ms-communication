package com.icthh.xm.tmf.ms.communication.config.tenant;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_EMAIL_SPEC_CONFIG_PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerConfigurationUnitTest {

    private static final String TENANT_KEY = "XM";
    private static final String CONFIG_EMAILS_PATH = "config/emails/uaa/";

    private TenantManager tenantManager;

    private TenantConfigProvisioner configProvisioner;

    @Spy
    private TenantManagerConfiguration configuration = new TenantManagerConfiguration();

    @Mock
    private TenantConfigRepository tenantConfigRepository;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ResourceLoader resourceLoader;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(applicationProperties.getDefaultEmailSpecificationPathPattern()).thenReturn("/config/tenants/{tenantName}/communication/default-email-spec.yml");

        configProvisioner = spy(configuration.tenantConfigProvisioner(tenantConfigRepository,
                                                                      applicationProperties,
                                                                      resourceLoader));

        tenantManager = configuration.tenantManager(configProvisioner);
    }

    @Test
    public void testCreateTenantConfigProvisioning() {
        tenantManager.createTenant(new Tenant().tenantKey(TENANT_KEY));
        verify(tenantConfigRepository).createConfigsFullPath(eq(TENANT_KEY), isExpectedConfigsByParams());
    }

    private List<Configuration> isExpectedConfigsByParams() {
        return argThat((List<Configuration> configs) ->
            containsConfig(configs,"/config/tenants/{tenantName}/communication/default-email-spec.yml", readResource(DEFAULT_EMAIL_SPEC_CONFIG_PATH))
                && containsConfig(configs,"/config/tenants/{tenantName}/communication/emails/uaa/activation/en.ftl", readResource(CONFIG_EMAILS_PATH + "activation/en.ftl"))
                && containsConfig(configs,"/config/tenants/{tenantName}/communication/emails/uaa/creation/en.ftl", readResource(CONFIG_EMAILS_PATH + "creation/en.ftl"))
                && containsConfig(configs,"/config/tenants/{tenantName}/communication/emails/uaa/passwordReset/en.ftl", readResource(CONFIG_EMAILS_PATH + "passwordReset/en.ftl"))
          );
    }

    private boolean containsConfig(List<Configuration> configs, String path, String content) {
        return configs.stream().anyMatch((cfg) -> isExpectedConfig(cfg, path, content));
    }

    private boolean isExpectedConfig(Configuration configuration, String path, String content) {
        return configuration.getPath().equals(path) && configuration.getContent().equals(content);
    }

    @SneakyThrows
    private String readResource(String location) {
        return IOUtils.toString(new ClassPathResource(location).getInputStream(), UTF_8);
    }

}
