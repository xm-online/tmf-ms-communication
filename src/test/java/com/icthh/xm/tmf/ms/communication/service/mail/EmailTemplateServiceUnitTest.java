package com.icthh.xm.tmf.ms.communication.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.CustomEmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.template.Configuration;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.test.binder.MessageCollectorAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = MessageCollectorAutoConfiguration.class)
@SpringBootTest(classes = {CommunicationApp.class, SecurityBeanOverrideConfiguration.class})
public class EmailTemplateServiceUnitTest {

    private static final String TENANT_KEY = "TEST";
    private static final String EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/email-spec.yml";
    private static final String CUSTOM_EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/custom-email-spec.yml";
    private static final String CUSTOM_EMAIL_TEMPLATES_PATH = "/config/tenants/TEST/communication/custom-emails/activation/firstTemplateKey/en.ftl";
    private static final String EMAIL_TEMPLATE_PATH = "/config/tenants/TEST/communication/emails/activation/secondTemplateKey/en.ftl";

    private EmailTemplateService subject;

    @Autowired
    private Configuration freeMarkerConfiguration;

    private EmailSpecService emailSpecService;

    private CustomEmailSpecService customEmailSpecService;

    @Autowired
    private TenantEmailTemplateService tenantEmailTemplateService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private ApplicationProperties applicationProperties;

    @MockBean
    private SmppService smppService;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        tenantContextHolder = mock(TenantContextHolder.class);
        mockTenant(TENANT_KEY);

        customEmailSpecService = new CustomEmailSpecService(applicationProperties);
        emailSpecService = new EmailSpecService(applicationProperties, customEmailSpecService, tenantContextHolder);
        subject = new EmailTemplateService(freeMarkerConfiguration,
            emailSpecService,
            tenantEmailTemplateService,
            tenantContextHolder);
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
    public void renderEmailContentReturnNullWhenContentNotValid() {
        RenderTemplateRequest renderTemplateRequest = createEmailTemplateDto("${subjectNotValid{", Map.of());

        subject.renderEmailContent(renderTemplateRequest);
    }

    @Test
    public void getTemplateDetailsByKeyWithCustomPath() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        String customEmailSpecificationConfig = loadFile("config/specs/custom-email-spec-2.yml");
        EmailTemplateSpec expectedEmailTemplate = readConfiguration(emailSpecificationConfig, EmailSpec.class).getEmails().get(0);
        CustomEmailTemplateSpec expectedCustomEmailTemplate = readConfiguration(customEmailSpecificationConfig, CustomEmailSpec.class).getEmails().get(0);
        String templateBody = loadFile("templates/customTemplate.ftl");

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        customEmailSpecService.onRefresh(CUSTOM_EMAIL_SPECIFICATION_PATH, customEmailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(CUSTOM_EMAIL_TEMPLATES_PATH, templateBody);

        TemplateDetails actual = subject.getTemplateDetailsByKey("firstTemplateKey");

        assertThat(actual.getContent()).isEqualTo(templateBody);
        assertThat(actual.getEmailForm()).isEqualTo(expectedEmailTemplate.getContextForm());
        assertThat(actual.getEmailSpec()).isEqualTo(expectedEmailTemplate.getContextSpec());
        assertThat(actual.getEmailData()).isEqualTo(expectedEmailTemplate.getContextExample());
        assertThat(actual.getSubject()).isEqualTo(expectedCustomEmailTemplate.getSubjectTemplate());
        assertThat(actual.getName()).isEqualTo(expectedCustomEmailTemplate.getName());
    }

    @Test
    public void getTemplateDetailsByKeyWithDefaultPath() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        EmailTemplateSpec expected = readConfiguration(emailSpecificationConfig, EmailSpec.class).getEmails().get(1);
        String templateBody = loadFile("templates/templateToRender.ftl");

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(EMAIL_TEMPLATE_PATH, templateBody);

        TemplateDetails actual = subject.getTemplateDetailsByKey("secondTemplateKey");

        assertThat(actual.getContent()).isEqualTo(templateBody);
        assertThat(actual.getEmailForm()).isEqualTo(expected.getContextForm());
        assertThat(actual.getEmailSpec()).isEqualTo(expected.getContextSpec());
        assertThat(actual.getEmailData()).isEqualTo(expected.getContextExample());
        assertThat(actual.getSubject()).isEqualTo(expected.getSubjectTemplate());
        assertThat(actual.getName()).isEqualTo(expected.getName());
    }

    @Test(expected = EntityNotFoundException.class)
    public void getTemplateDetailsByKeyThrowEntityNotFoundWhenTemplateKeyNotValid() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);

        subject.getTemplateDetailsByKey("notValidKey");
    }

    private RenderTemplateRequest createEmailTemplateDto(String content, Map model) {
        RenderTemplateRequest renderTemplateRequest = new RenderTemplateRequest();
        renderTemplateRequest.setContent(content);
        renderTemplateRequest.setModel(model);
        return renderTemplateRequest;
    }

    private void mockTenant(String tenant) {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(tenant)));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContextHolder.getTenantKey()).thenReturn(tenant);
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @SneakyThrows
    private <T> T readConfiguration(String config, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(config, type);
    }
}
