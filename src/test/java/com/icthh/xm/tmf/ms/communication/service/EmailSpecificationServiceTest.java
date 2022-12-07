package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomerEmailTemplateSpec;
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
import java.util.stream.Collectors;

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
    private EmailSpecificationService emailSpecificationService;

    private TenantContextHolder tenantContextHolder;

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Before
    public void setUp() {
        tenantContextHolder = mock(TenantContextHolder.class);
        mockTenant("TEST");
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setEmailSpecificationPathPattern(EMAIL_SPECIFICATION_PATH_PATTERN);
        applicationProperties.setCustomEmailSpecificationPathPattern(CUSTOMER_EMAIL_SPECIFICATION_PATH_PATTERN);

        emailSpecificationService = new EmailSpecificationService(applicationProperties, tenantContextHolder);
    }

    @Test
    public void getEmailSpecList() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        String customEmailSpecificationConfig = loadFile("config/specs/customer-email-spec.yml");
        emailSpecificationService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        emailSpecificationService.onRefresh(CUSTOMER_EMAIL_SPECIFICATION_PATH, customEmailSpecificationConfig);
        List<EmailTemplateSpec> expectedEmailSpecList = List.of(
            getCustomerEmailTemplateSpecList(customEmailSpecificationConfig).get(0),
            getDefaultEmailTemplateSpecList(emailSpecificationConfig).get(1)
        ).stream().sorted().collect(Collectors.toList());

        List<EmailTemplateSpec> emailSpecList = emailSpecificationService.getEmailSpecList().stream().sorted().collect(Collectors.toList());
        assertEquals(expectedEmailSpecList, emailSpecList);
    }

    @Test
    @SneakyThrows
    public void getEmailSpecListWithoutDefault() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        String customEmailSpecificationConfig = loadFile("config/specs/customer-email-spec-2.yml");
        emailSpecificationService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        emailSpecificationService.onRefresh(CUSTOMER_EMAIL_SPECIFICATION_PATH, customEmailSpecificationConfig);
        List<EmailTemplateSpec> expectedEmailSpecList = List.of(
            getCustomerEmailTemplateSpecList(customEmailSpecificationConfig).get(0),
            getDefaultEmailTemplateSpecList(emailSpecificationConfig).get(1)
        ).stream().sorted().collect(Collectors.toList());

        List<EmailTemplateSpec> emailSpecList = emailSpecificationService.getEmailSpecList().stream().sorted().collect(Collectors.toList());
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
    private List<EmailTemplateSpec> getCustomerEmailTemplateSpecList(String config) {
        Map<String, List<EmailTemplateSpec>> customerEmailSpec = mapper.readValue(config, new TypeReference<Map<String, List<CustomerEmailTemplateSpec>>>(){});
        return customerEmailSpec.get("emails");
    }

    @SneakyThrows
    public String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }
}
