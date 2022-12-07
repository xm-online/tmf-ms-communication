package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class EmailSpecificationServiceTest {

    private static final String EMAIL_SPECIFICATION_PATH_PATTERN = "/config/tenants/{tenantName}/communication/email-spec.yml";
    private static final String CUSTOMER_EMAIL_SPECIFICATION_PATH_PATTERN = "/config/tenants/{tenantName}/communication/customer-email-spec.yml";
    private static final String EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/email-spec.yml";
    private static final String CUSTOMER_EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/customer-email-spec.yml";

    @Spy
    @InjectMocks
    private EmailSpecService emailSpecService;

    private CustomerEmailSpecService customerEmailSpecService;

    private TenantContextHolder tenantContextHolder;

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Before
    public void setUp() {
        tenantContextHolder = mock(TenantContextHolder.class);
        mockTenant("TEST");
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setEmailSpecificationPathPattern(EMAIL_SPECIFICATION_PATH_PATTERN);
        applicationProperties.setCustomEmailSpecificationPathPattern(CUSTOMER_EMAIL_SPECIFICATION_PATH_PATTERN);

        customerEmailSpecService = new CustomerEmailSpecService(applicationProperties);
        emailSpecService = new EmailSpecService(applicationProperties, customerEmailSpecService, tenantContextHolder);
    }

    @Test
    public void getEmailSpecList() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        String customEmailSpecificationConfig = loadFile("config/specs/customer-email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        customerEmailSpecService.onRefresh(CUSTOMER_EMAIL_SPECIFICATION_PATH, customEmailSpecificationConfig);
        List<EmailTemplateSpec> expectedEmailSpecList = getDefaultEmailTemplateSpecList(emailSpecificationConfig);
        expectedEmailSpecList.get(0).setSubjectTemplate("Custom subject 1");

        List<EmailTemplateSpec> emailSpecList = emailSpecService.getEmailSpec().getEmails();
        assertEquals(expectedEmailSpecList, emailSpecList);
    }

    @Test
    @SneakyThrows
    public void getEmailSpecListWithoutDefault() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        String customEmailSpecificationConfig = loadFile("config/specs/customer-email-spec-2.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        customerEmailSpecService.onRefresh(CUSTOMER_EMAIL_SPECIFICATION_PATH, customEmailSpecificationConfig);
        List<EmailTemplateSpec> expectedEmailSpecList = getDefaultEmailTemplateSpecList(emailSpecificationConfig);
        expectedEmailSpecList.get(0).setSubjectTemplate("Custom subject 1");

        List<EmailTemplateSpec> emailSpecList = emailSpecService.getEmailSpec().getEmails();
        assertEquals(expectedEmailSpecList, emailSpecList);
    }

    public void mockTenant(String tenant) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenant)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContextHolder.getTenantKey()).thenReturn(tenant);
    }

    @SneakyThrows
    private List<EmailTemplateSpec> getDefaultEmailTemplateSpecList(String config) {
        Map<String, List<EmailTemplateSpec>> defaultEmailSpec = mapper.readValue(config, new TypeReference<Map<String, List<EmailTemplateSpec>>>(){});
        return defaultEmailSpec.get("emails");
    }

    @SneakyThrows
    public String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }
}
