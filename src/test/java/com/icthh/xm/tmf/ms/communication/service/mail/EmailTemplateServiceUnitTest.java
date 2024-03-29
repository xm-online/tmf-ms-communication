package com.icthh.xm.tmf.ms.communication.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateMultiLangDetails;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.CustomEmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.mapper.TemplateDetailsMapper;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.cache.StringTemplateLoader;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_LANGUAGE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = MessageCollectorAutoConfiguration.class)
@SpringBootTest(classes = {CommunicationApp.class, SecurityBeanOverrideConfiguration.class})
public class EmailTemplateServiceUnitTest {

    private static final String TENANT_KEY = "TEST";
    private static final String UPDATED_SUBJECT_NAME = "updated subject name";
    private static final String UPDATED_EMAIL_FROM = "updated email from";
    private static final String EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/email-spec.yml";
    private static final String CUSTOM_EMAIL_SPECIFICATION_PATH = "/config/tenants/TEST/communication/custom-email-spec.yml";
    private static final String CUSTOM_EMAILS_TEMPLATES_PATH = "/config/tenants/TEST/communication/custom-emails/";
    private static final String CUSTOM_EMAIL_TEMPLATES_PATH = "/config/tenants/TEST/communication/custom-emails/activation/firstTemplateKey/en.ftl";
    private static final String FIRST_EMAIL_TEMPLATE_PATH = "/config/tenants/TEST/communication/emails/activation/firstTemplateKey/en.ftl";
    private static final String FIRST_EMAIL_TEMPLATE_PATH_UK = "/config/tenants/TEST/communication/emails/activation/firstTemplateKey/uk.ftl";
    private static final String SECOND_EMAIL_TEMPLATE_PATH = "/config/tenants/TEST/communication/emails/activation/secondTemplateKey/en.ftl";
    private static final String BASE_EMAIL_TEMPLATE_PATH = "/config/tenants/TEST/communication/emails/activation/base/en.ftl";
    public static final String UK_LANG = "uk";
    private TenantContextHolder tenantContextHolder;

    private EmailSpecService emailSpecService;

    private EmailTemplateService subject;

    private CustomEmailSpecService customEmailSpecService;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Mock
    private CommonConfigRepository commonConfigRepository;

    @Autowired
    private freemarker.template.Configuration freeMarkerConfiguration;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private TemplateDetailsMapper templateDetailsMapper;

    @Autowired
    private TenantEmailTemplateService tenantEmailTemplateService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MultiTenantLangStringTemplateLoaderService multiTenantLangStringTemplateLoaderService;

    private StringTemplateLoader stringTemplateLoader;

    @MockBean
    private SmppService smppService;

    @MockBean
    private RestTemplate restTemplate;


    @Before
    public void setup() {
        tenantContextHolder = mock(TenantContextHolder.class);
        mockTenant(TENANT_KEY);

        customEmailSpecService = new CustomEmailSpecService(applicationProperties, tenantContextHolder);
        emailSpecService = new EmailSpecService(applicationProperties, customEmailSpecService, tenantContextHolder);

        subject = new EmailTemplateService(freeMarkerConfiguration,
            emailSpecService,
            tenantEmailTemplateService,
            customEmailSpecService,
            commonConfigRepository,
            tenantContextHolder,
            objectMapper,
            templateDetailsMapper,
            stringTemplateLoader,
            multiTenantLangStringTemplateLoaderService);
    }

    @Test
    public void renderEmailContent() {
        String content = loadFile("templates/templateToRender.ftl");
        Map<String, Object> model = Map.of("title", "Test", "baseUrl", "testUrl", "user",
            Map.of("firstName", "Name", "lastName", "Surname", "resetKey", "key"));
        String expectedContent = loadFile("templates/renderedTemplate.html");
        RenderTemplateRequest renderTemplateRequest = createEmailTemplateDto(content, model, DEFAULT_LANGUAGE);

        String actual = subject.renderEmailContent(renderTemplateRequest).getContent();

        verify(multiTenantLangStringTemplateLoaderService).getTemplateLoader(TENANT_KEY, DEFAULT_LANGUAGE);
        verifyNoMoreInteractions(multiTenantLangStringTemplateLoaderService);

        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(expectedContent);
    }

    @Test(expected = RenderTemplateException.class)
    public void renderEmailContentReturnNullWhenContentNotValid() {
        RenderTemplateRequest renderTemplateRequest = createEmailTemplateDto("${subjectNotValid{", Map.of(), DEFAULT_LANGUAGE);

        subject.renderEmailContent(renderTemplateRequest);
    }

    private RenderTemplateRequest createEmailTemplateDto(String content, Map model, String lang) {
        RenderTemplateRequest renderTemplateRequest = new RenderTemplateRequest();
        renderTemplateRequest.setContent(content);
        renderTemplateRequest.setModel(model);
        renderTemplateRequest.setLang(lang);
        renderTemplateRequest.setTemplatePath(TENANT_KEY + "/test/" + DEFAULT_LANGUAGE);
        return renderTemplateRequest;
    }

    @Test
    public void getTemplateDetailsByKeyWithCustomPath() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        String customEmailSpecificationConfig = loadFile("config/specs/custom-email-spec-2.yml");
        EmailTemplateSpec expectedEmailTemplate = readConfiguration(emailSpecificationConfig, EmailSpec.class).getEmails().get(0);
        CustomEmailTemplateSpec expectedCustomEmailTemplate = readConfiguration(customEmailSpecificationConfig, CustomEmailSpec.class).getEmails().get(0);
        String templateBody = loadFile("templates/customTemplate.ftl");
        String templatePath = TENANT_KEY + "/" + expectedEmailTemplate.getTemplatePath() + "/" + DEFAULT_LANGUAGE;

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        customEmailSpecService.onRefresh(CUSTOM_EMAIL_SPECIFICATION_PATH, customEmailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(CUSTOM_EMAIL_TEMPLATES_PATH, templateBody);

        TemplateDetails actual = subject.getTemplateDetailsByKey("firstTemplateKey", DEFAULT_LANGUAGE);

        assertThat(actual.getContent()).isEqualTo(templateBody);
        assertThat(actual.getContextForm()).isEqualTo(expectedEmailTemplate.getContextForm());
        assertThat(actual.getContextSpec()).isEqualTo(expectedEmailTemplate.getContextSpec());
        assertThat(actual.getContextExample()).isEqualTo(expectedEmailTemplate.getContextExample());
        assertThat(actual.getSubjectTemplate()).isEqualTo(expectedCustomEmailTemplate.getSubjectTemplate().get(DEFAULT_LANGUAGE));
        assertThat(actual.getEmailFrom()).isEqualTo(expectedCustomEmailTemplate.getEmailFrom().get(DEFAULT_LANGUAGE));
        assertThat(actual.getTemplatePath()).isEqualTo(templatePath);
    }

    @Test
    public void getTemplateDetailsByKeyWithDefaultPath() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        EmailTemplateSpec expected = readConfiguration(emailSpecificationConfig, EmailSpec.class).getEmails().get(1);
        String templateBody = loadFile("templates/templateToRender.ftl");
        String templatePath = TENANT_KEY + "/" + expected.getTemplatePath() + "/" + DEFAULT_LANGUAGE;

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(SECOND_EMAIL_TEMPLATE_PATH, templateBody);

        TemplateDetails actual = subject.getTemplateDetailsByKey("secondTemplateKey", DEFAULT_LANGUAGE);

        assertThat(actual.getContent()).isEqualTo(templateBody);
        assertThat(actual.getContextForm()).isEqualTo(expected.getContextForm());
        assertThat(actual.getContextSpec()).isEqualTo(expected.getContextSpec());
        assertThat(actual.getContextExample()).isEqualTo(expected.getContextExample());
        assertThat(actual.getSubjectTemplate()).isEqualTo(expected.getSubjectTemplate().get(DEFAULT_LANGUAGE));
        assertThat(actual.getEmailFrom()).isEqualTo(expected.getEmailFrom().get(DEFAULT_LANGUAGE));
        assertThat(actual.getTemplatePath()).isEqualTo(templatePath);
    }

    @Test(expected = EntityNotFoundException.class)
    public void getTemplateDetailsByKeyThrowEntityNotFoundWhenTemplateKeyNotValid() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);

        subject.getTemplateDetailsByKey("notValidKey", DEFAULT_LANGUAGE);
    }

    @Test(expected = EntityNotFoundException.class)
    public void getTemplateMultiLangDetailsByKeyThrowEntityNotFoundWhenTemplateKeyNotValid() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        subject.getTemplateMultiLangDetailsByKey("notValidKey");
    }

    @Test
    public void getTemplateMultiLangDetailsByKeyWithCustomPath() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        String customEmailSpecificationConfig = loadFile("config/specs/custom-email-spec-2.yml");
        EmailTemplateSpec expectedEmailTemplate = readConfiguration(emailSpecificationConfig, EmailSpec.class).getEmails().get(0);
        CustomEmailTemplateSpec expectedCustomEmailTemplate = readConfiguration(customEmailSpecificationConfig, CustomEmailSpec.class).getEmails().get(0);
        String templateBody = loadFile("templates/customTemplate.ftl");

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        customEmailSpecService.onRefresh(CUSTOM_EMAIL_SPECIFICATION_PATH, customEmailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(CUSTOM_EMAIL_TEMPLATES_PATH, templateBody);
        tenantEmailTemplateService.onRefresh(FIRST_EMAIL_TEMPLATE_PATH_UK, "UK_TEMPLATE_BODY");

        TemplateMultiLangDetails actual = subject.getTemplateMultiLangDetailsByKey("firstTemplateKey");

        assertThat(actual.getContent().get(DEFAULT_LANGUAGE)).isEqualTo(templateBody);
        assertThat(actual.getContent().get(UK_LANG)).isEqualTo("UK_TEMPLATE_BODY");
        assertThat(actual.getContextForm()).isEqualTo(expectedEmailTemplate.getContextForm());
        assertThat(actual.getContextSpec()).isEqualTo(expectedEmailTemplate.getContextSpec());
        assertThat(actual.getContextExample()).isEqualTo(expectedEmailTemplate.getContextExample());
        assertThat(actual.getSubjectTemplate().get(DEFAULT_LANGUAGE)).isEqualTo(expectedCustomEmailTemplate.getSubjectTemplate().get(DEFAULT_LANGUAGE));
        assertThat(actual.getEmailFrom().get(DEFAULT_LANGUAGE)).isEqualTo(expectedCustomEmailTemplate.getEmailFrom().get(DEFAULT_LANGUAGE));
        assertThat(actual.getSubjectTemplate().get(UK_LANG)).isEqualTo(expectedCustomEmailTemplate.getSubjectTemplate().get(UK_LANG));
        assertThat(actual.getEmailFrom().get(UK_LANG)).isEqualTo(expectedCustomEmailTemplate.getEmailFrom().get(UK_LANG));
    }

    @Test
    public void getTemplateMultiLangDetailsByWhenByOneLangTemplateNotExists() {
        String templateBody = "TEMPLATE+BODY";
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        EmailTemplateSpec expectedEmailTemplate = readConfiguration(emailSpecificationConfig, EmailSpec.class).getEmails().get(0);

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(CUSTOM_EMAIL_TEMPLATES_PATH, templateBody);

        TemplateMultiLangDetails actual = subject.getTemplateMultiLangDetailsByKey("firstTemplateKey");

        assertThat(actual.getContent().get(DEFAULT_LANGUAGE)).isEqualTo(templateBody);
        assertThat(actual.getContent().get(UK_LANG)).isNullOrEmpty();
        assertThat(actual.getContextForm()).isEqualTo(expectedEmailTemplate.getContextForm());
        assertThat(actual.getContextSpec()).isEqualTo(expectedEmailTemplate.getContextSpec());
        assertThat(actual.getContextExample()).isEqualTo(expectedEmailTemplate.getContextExample());
        assertThat(actual.getSubjectTemplate().get(DEFAULT_LANGUAGE)).isEqualTo(expectedEmailTemplate.getSubjectTemplate().get(DEFAULT_LANGUAGE));
        assertThat(actual.getSubjectTemplate().get(UK_LANG)).isEqualTo(expectedEmailTemplate.getSubjectTemplate().get(UK_LANG));
        assertThat(actual.getEmailFrom().get(DEFAULT_LANGUAGE)).isEqualTo(expectedEmailTemplate.getEmailFrom().get(DEFAULT_LANGUAGE));
        assertThat(actual.getEmailFrom().get(UK_LANG)).isEqualTo(expectedEmailTemplate.getEmailFrom().get(UK_LANG));
    }

    @Test
    public void getTemplateDetailsByKeyReturnDefaultSubjectWhenLangKeyNotExists() {
        String itTemplate = "/config/tenants/TEST/communication/emails/activation/secondTemplateKey/it.ftl";
        String emailSpecificationConfig = loadFile("config/specs/email-spec.yml");
        EmailTemplateSpec expected = readConfiguration(emailSpecificationConfig, EmailSpec.class).getEmails().get(1);
        String templateBody = loadFile("templates/templateToRender.ftl");

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(itTemplate, templateBody);

        TemplateDetails actual = subject.getTemplateDetailsByKey("secondTemplateKey", "it");

        assertThat(actual.getContent()).isEqualTo(templateBody);
        assertThat(actual.getContextForm()).isEqualTo(expected.getContextForm());
        assertThat(actual.getContextSpec()).isEqualTo(expected.getContextSpec());
        assertThat(actual.getContextExample()).isEqualTo(expected.getContextExample());
        assertThat(actual.getSubjectTemplate()).isEqualTo(expected.getSubjectTemplate().get(DEFAULT_LANGUAGE));
        assertThat(actual.getEmailFrom()).isEqualTo(expected.getEmailFrom().get(DEFAULT_LANGUAGE));
    }

    @Test
    public void getTemplateDetailsByKeyWithDepends() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec-depends.yml");
        String contextExampleExpected = loadFile("config/specs/context-example-depends-expected.json");
        String contextSpecExpected = loadFile("config/specs/context-spec-depends-expected.json");
        String contextFormExpected = loadFile("config/specs/context-form-depends-expected.json");
        String templateBody = loadFile("templates/templateToRender.ftl");

        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);
        tenantEmailTemplateService.onRefresh(BASE_EMAIL_TEMPLATE_PATH, templateBody);
        tenantEmailTemplateService.onRefresh(FIRST_EMAIL_TEMPLATE_PATH, templateBody);
        tenantEmailTemplateService.onRefresh(SECOND_EMAIL_TEMPLATE_PATH, templateBody);

        TemplateDetails actual = subject.getTemplateDetailsByKey("secondTemplateKey", DEFAULT_LANGUAGE);

        assertThat(toJsonNode(actual.getContextExample())).isEqualTo(toJsonNode(contextExampleExpected));
        assertThat(toJsonNode(actual.getContextSpec())).isEqualTo(toJsonNode(contextSpecExpected));
        assertThat(toJsonNode(actual.getContextForm())).isEqualTo(toJsonNode(contextFormExpected));
    }

    @Test
    public void testUpdateTemplate() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec-updated.yml");
        String templateKey = "firstTemplateKey";
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);

        UpdateTemplateRequest updateTemplateRequest = createUpdateRequestTemplate();

        when(commonConfigRepository.getConfig(eq(null), any(Collection.class))).thenReturn(null);

        subject.updateTemplate(templateKey, "en", updateTemplateRequest);

        verify(commonConfigRepository, times(2)).getConfig(eq(null), any(Collection.class));

        ArgumentCaptor<Configuration> configCaptor = ArgumentCaptor.forClass(Configuration.class);
        verify(commonConfigRepository, times(2)).updateConfigFullPath(configCaptor.capture(), eq(null));
        List<Configuration> configs = configCaptor.getAllValues();

        assertTrue(isExpectedSpec(configs.get(0), templateKey));
        assertTrue(isExpectedEmail(configs.get(1)));

        verifyNoMoreInteractions(commonConfigRepository);
    }

    @Test
    public void testUpdateTemplateWhenEmailFromAndSubjectDoesNotExist() {
        String emailSpecificationConfig = loadFile("config/specs/email-spec-updated.yml");
        String templateKey = "templateWithoutEmailFromAndSubject";
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecificationConfig);

        UpdateTemplateRequest updateTemplateRequest = createUpdateRequestTemplate();

        when(commonConfigRepository.getConfig(eq(null), any(Collection.class))).thenReturn(null);

        subject.updateTemplate(templateKey, "en", updateTemplateRequest);

        verify(commonConfigRepository, times(2)).getConfig(eq(null), any(Collection.class));

        ArgumentCaptor<Configuration> configCaptor = ArgumentCaptor.forClass(Configuration.class);
        verify(commonConfigRepository, times(2)).updateConfigFullPath(configCaptor.capture(), eq(null));
        List<Configuration> configs = configCaptor.getAllValues();

        assertTrue(isExpectedSpec(configs.get(0), templateKey));
        assertTrue(isExpectedEmail(configs.get(1)));

        verifyNoMoreInteractions(commonConfigRepository);
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
        return yamlMapper.readValue(config, type);
    }

    private UpdateTemplateRequest createUpdateRequestTemplate() {
        UpdateTemplateRequest updateTemplateRequest = new UpdateTemplateRequest();
        updateTemplateRequest.setSubjectTemplate(UPDATED_SUBJECT_NAME);
        updateTemplateRequest.setEmailFrom(UPDATED_EMAIL_FROM);
        updateTemplateRequest.setContent(loadFile("templates/updatedTemplate.ftl"));

        return updateTemplateRequest;
    }

    private boolean isExpectedSpec(Configuration configuration, String templateKey) {
        EmailSpec emailSpec = readConfiguration(configuration.getContent(), EmailSpec.class);
        EmailTemplateSpec emailTemplateSpec = emailSpec.getEmails().stream()
            .filter((spec) -> spec.getTemplateKey().equals(templateKey)).findFirst().get();
        return configuration.getPath().equals(CUSTOM_EMAIL_SPECIFICATION_PATH)
            && emailTemplateSpec.getSubjectTemplate().get("en").equals(UPDATED_SUBJECT_NAME)
            && emailTemplateSpec.getEmailFrom().get("en").equals(UPDATED_EMAIL_FROM);
    }

    private boolean isExpectedEmail(Configuration configuration) {
        return configuration.getPath().equals(CUSTOM_EMAILS_TEMPLATES_PATH + "uaa/activate/en.ftl")
            && configuration.getContent().equals(loadFile("templates/updatedTemplate.ftl"));
    }

    @SneakyThrows
    private JsonNode toJsonNode(String json) {
        return objectMapper.readValue(json, JsonNode.class);
    }

}
