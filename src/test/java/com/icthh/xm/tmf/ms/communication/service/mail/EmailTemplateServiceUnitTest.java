package com.icthh.xm.tmf.ms.communication.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.CustomEmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.test.binder.MessageCollectorAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.tmf.ms.communication.config.Constants.API_PRIVATE_CONFIG;
import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_EMAIL_SPEC_CONFIG_PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = MessageCollectorAutoConfiguration.class)
@SpringBootTest(classes = {CommunicationApp.class, SecurityBeanOverrideConfiguration.class})
public class EmailTemplateServiceUnitTest {

    private static final String TENANT_KEY = "TEST";
    private static final String UPDATED_TEMPLATE_NAME = "updated template name";
    private static final String UPDATED_SUBJECT_NAME = "updated subject name";
    private static final String EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/email-spec.yml";
    private static final String CUSTOM_EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/custom-email-spec.yml";
    private static final String EMAIL_SPECIFICATION_PATH_PATTERN = "/config/tenants/{tenantName}/communication/email-spec.yml";
    private static final String CUSTOM_EMAIL_SPECIFICATION_PATH_PATTERN = "/config/tenants/{tenantName}/communication/custom-email-spec.yml";
    private static final String CUSTOM_EMAIL_TEMPLATES_PATH = "/config/tenants/TEST/communication/custom-emails/";

    private TenantContextHolder tenantContextHolder;

    private EmailSpecService emailSpecService;

    private EmailTemplateService subject;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @Mock
    private ApplicationProperties applicationProperties;

    @MockBean
    private SmppService smppService;

    @MockBean
    private RestTemplate restTemplate;
    @Autowired
    private freemarker.template.Configuration freeMarkerConfiguration;


    @Before
    public void init() {
        tenantContextHolder = mock(TenantContextHolder.class);
        mockTenant(TENANT_KEY);

        when(applicationProperties.getEmailSpecificationPathPattern()).thenReturn(EMAIL_SPECIFICATION_PATH_PATTERN);
        when(applicationProperties.getCustomEmailSpecificationPathPattern()).thenReturn(CUSTOM_EMAIL_SPECIFICATION_PATH_PATTERN);

        CustomEmailSpecService customEmailSpecService = new CustomEmailSpecService(applicationProperties);
        emailSpecService = new EmailSpecService(applicationProperties, customEmailSpecService, tenantContextHolder);

        subject = new EmailTemplateService(freeMarkerConfiguration, emailSpecService, tenantConfigRepository, tenantContextHolder);
    }

    @Test
    public void renderEmailContent() {
        String content = loadFile("templates/templateToRender.ftl");
        Map<String, Object> model = Map.of("title", "Test", "baseUrl", "testUrl", "user",
            Map.of("firstName", "Name", "lastName", "Surname", "resetKey", "key"));
        String expectedContent = loadFile("templates/renderedTemplate.html");
        RenderTemplateRequest renderTemplateRequest = createEmailTemplateDto(content, model);

        String actual = subject.renderEmailContent(renderTemplateRequest).getContent();

        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(expectedContent);
    }

    @Test(expected = RenderTemplateException.class)
    public void renderEmailContentReturnNullWhenContentNotValid(){
        RenderTemplateRequest renderTemplateRequest = createEmailTemplateDto("${subjectNotValid{", Map.of());

        subject.renderEmailContent(renderTemplateRequest);
    }

    @Test
    public void testUpdateTemplate() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);

        UpdateTemplateRequest updateTemplateRequest = createUpdateRequestTemplate();

        subject.updateTemplate("firstTemplateKey", updateTemplateRequest);

        verify(tenantConfigRepository).updateConfigFullPath(eq(TENANT_KEY), eq(API_PRIVATE_CONFIG), isExpectedSpecConfigByParams());
        verify(tenantConfigRepository).updateConfigFullPath(eq(TENANT_KEY), eq(API_PRIVATE_CONFIG), isExpectedEmailConfigByParams());
        verifyNoMoreInteractions(tenantConfigRepository);
    }

    private RenderTemplateRequest createEmailTemplateDto(String content, Map model) {
        RenderTemplateRequest renderTemplateRequest = new RenderTemplateRequest();
        renderTemplateRequest.setContent(content);
        renderTemplateRequest.setModel(model);
        return renderTemplateRequest;
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    private void mockTenant(String tenant) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenant)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContextHolder.getTenantKey()).thenReturn(tenant);
    }

    private UpdateTemplateRequest createUpdateRequestTemplate() {
        UpdateTemplateRequest updateTemplateRequest = new UpdateTemplateRequest();
        updateTemplateRequest.setTemplateName(UPDATED_TEMPLATE_NAME);
        updateTemplateRequest.setTemplateSubject(UPDATED_SUBJECT_NAME);
        updateTemplateRequest.setContent(loadFile("templates/updatedTemplate.ftl"));

        return updateTemplateRequest;
    }

    private String isExpectedSpecConfigByParams() {
        return argThat((String config) -> {
                Configuration configuration = readConfiguration(config, Configuration.class);
                EmailSpec emailSpec = readConfiguration(configuration.getContent(), EmailSpec.class);
                EmailTemplateSpec emailTemplateSpec = emailSpec.getEmails().stream()
                    .filter((spec) -> spec.getTemplateKey().equals("firstTemplateKey")).findFirst().get();
                return configuration.getPath().equals(CUSTOM_EMAIL_SPECIFICATION_PATH)
                    && emailTemplateSpec.getName().equals(UPDATED_TEMPLATE_NAME)
                    && emailTemplateSpec.getSubjectTemplate().equals(UPDATED_SUBJECT_NAME);
            }
        );
    }

    private String isExpectedEmailConfigByParams() {
        return argThat((String config) -> {
                Configuration configuration = readConfiguration(config, Configuration.class);
                return configuration.getPath().equals(CUSTOM_EMAIL_TEMPLATES_PATH + "uaa/emails/en/firstTemplateKey.ftl")
                    && configuration.getContent().equals(loadFile("templates/updatedTemplate.ftl"));
            }
        );
    }

    @SneakyThrows
    private <T> T readConfiguration(String config, Class<T> type) {
        return mapper.readValue(config, type);
    }
}
