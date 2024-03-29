package com.icthh.xm.tmf.ms.communication.service.mail;

import static com.icthh.xm.tmf.ms.communication.service.mail.EmailTemplateServiceUnitTest.loadFile;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.CommunicationTenantConfigService;
import com.icthh.xm.tmf.ms.communication.config.CommunicationTenantConfigService.CommunicationTenantConfig.MailSetting;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.EmailReceiver;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.mail.internet.MimeMessage;

import freemarker.cache.StringTemplateLoader;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.test.binder.MessageCollectorAutoConfiguration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.AopTestUtils;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = "non-async")
@EnableAutoConfiguration(exclude = MessageCollectorAutoConfiguration.class)
@SpringBootTest(classes = {CommunicationApp.class, SecurityBeanOverrideConfiguration.class})
public class MailServiceUnitTest {

    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String EMAIL = "email@email.com";
    private static final String FROM = "from";
    private static final String RID = "rid";
    public static final String TENANT_NAME = "RESINTTEST";
    public static final String TEMPLATE_KEY = "firstTemplateKey";
    public static final String MACRO_TEMPLATE_KEY = "/macroTemplateKey";
    private static final String EMAIL_SPECIFICATION_PATH_PATTERN = "/config/tenants/{tenantName}/communication/email-spec.yml";
    private static final String CUSTOM_EMAIL_SPECIFICATION_PATH_PATTERN = "/config/tenants/{tenantName}/communication/custom-email-spec.yml";
    private static final String EMAIL_SPECIFICATION_PATH = "/config/tenants/RESINTTEST/communication/email-spec.yml";
    private static final String CUSTOM_EMAIL_SPECIFICATION_PATH = "/config/tenants/RESINTTEST/communication/custom-email-spec.yml";
    public static final Map<String, String> MULTILINGUAL_SUBJECT = Map.of("en", "subject", "uk", "тема");

    @Autowired
    private MailService mailService;

    @Autowired
    private TenantEmailTemplateService templateService;

    @Autowired
    private StringTemplateLoader templateLoader;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private MailProviderService mailProviderService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private CommunicationTenantConfigService communicationTenantConfigService;

    @Mock
    private XmAuthenticationContext context;

    @MockBean
    private SmppService smppService;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private EmailSpecService emailSpecService;

    @SneakyThrows
    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_NAME);
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));
        when(mailProviderService.getJavaMailSender(any())).thenReturn(javaMailSender);
        MimeMessage mimeMessageMock = mock(MimeMessage.class);
        doReturn(mimeMessageMock).when(javaMailSender).createMimeMessage();
        doNothing().when(javaMailSender).send(mimeMessageMock);

        MailSetting mailSetting = new MailSetting(
            TEMPLATE_NAME,
            Map.of(ENGLISH.getLanguage(), "Subject with ${variable1}"),
            Map.of(ENGLISH.getLanguage(), "test@communication.com")
        );
        communicationTenantConfigService.getCommunicationTenantConfig().getMailSettings().add(mailSetting);

        applicationProperties.setEmailSpecificationPathPattern(EMAIL_SPECIFICATION_PATH_PATTERN);
        applicationProperties.setCustomEmailSpecificationPathPattern(CUSTOM_EMAIL_SPECIFICATION_PATH_PATTERN);
    }

    @Test
    public void testComplexTemplateEmail() {
        String mainPath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "/en.ftl";
        String basePath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "-BASE/en.ftl";
        String body = "<#import \"/" + TENANT_NAME + "/" + TEMPLATE_NAME + "-BASE/en\" as main>OTHER_<@main.body>_CUSTOM_</@main.body>";
        String base = "<#macro body>BASE_START<#nested>BASE_END</#macro>";
        templateService.onRefresh(mainPath, body);
        templateService.onRefresh(basePath, base);
        MailService spiedMailService = spy((MailService)AopTestUtils.getUltimateTargetObject(mailService));
        spiedMailService.sendEmailFromTemplate(TenantKey.valueOf(TENANT_NAME),
            ENGLISH,
            TEMPLATE_NAME,
            SUBJECT,
            EMAIL,
            Map.of(
                "variable1", "value1",
                "variable2", "value2"
            ),
            RID, FROM);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(spiedMailService).sendEmail(
            eq(new EmailReceiver(EMAIL)), // to
            captor.capture(), // subject
            eq("OTHER_BASE_START_CUSTOM_BASE_END"),
            captor.capture(), // from
            any(),
            any()
        );

        List<String> allValues = captor.getAllValues();
        assertThat(allValues).containsExactly("Subject with value1", "test@communication.com");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailOnEmailSend() {
        String mainPath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "/en.ftl";
        String basePath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "-BASE/en.ftl";
        String body = "<#import \"/" + TENANT_NAME + "/" + TEMPLATE_NAME + "-BASE/en\" as main>OTHER_<@main.body>_CUSTOM_</@main.body>";
        String base = "<#macro body>BASE_START<#nested>BASE_END</#macro>";
        templateService.onRefresh(mainPath, body);
        templateService.onRefresh(basePath, base);
        applicationProperties.getEmail().setFailOnError(true);
        doThrow(new MailSendException("Simulated send failure")).when(javaMailSender).send(any(MimeMessage.class));

        MailService spiedMailService = spy((MailService) AopTestUtils.getUltimateTargetObject(mailService));
        spiedMailService.sendEmailFromTemplate(TenantKey.valueOf(TENANT_NAME),
            ENGLISH,
            TEMPLATE_NAME,
            SUBJECT,
            EMAIL,
            Map.of(
                "variable1", "value1",
                "variable2", "value2"
            ),
            RID, FROM);
    }

    @Test
    public void testComplexTemplateEmailByTemplateKey() {
        String mainPath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "/en.ftl";
        String basePath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "-BASE/en.ftl";
        String body = "<#import \"" + MACRO_TEMPLATE_KEY + "\" as main>OTHER_<@main.body>_CUSTOM_</@main.body>";
        String base = "<#macro body>BASE_START<#nested>BASE_END</#macro>";

        String emailSpecConfig = loadFile("config/specs/email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecConfig);

        templateService.onRefresh(mainPath, body);
        templateService.onRefresh(basePath, base);

        MailService spiedMailService = spy((MailService)AopTestUtils.getUltimateTargetObject(mailService));
        spiedMailService.sendEmailFromTemplate(TenantKey.valueOf(TENANT_NAME),
            ENGLISH,
            TEMPLATE_NAME,
            SUBJECT,
            EMAIL,
            Map.of(
                "variable1", "value1",
                "variable2", "value2"
            ),
            RID, FROM);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(spiedMailService).sendEmail(
            eq(new EmailReceiver(EMAIL)), // to
            captor.capture(), // subject
            eq("OTHER_BASE_START_CUSTOM_BASE_END"),
            captor.capture(), // from
            any(),
            any()
        );

        List<String> allValues = captor.getAllValues();
        assertThat(allValues).containsExactly("Subject with value1", "test@communication.com");
    }

    @Test
    public void testEmailTemplateCreateConfig() {
        String emailPath = "/config/tenants/XM/communication/emails/activation/subfolder/en.ftl";
        String customEmailPath = "/config/tenants/XM/communication/custom-emails/register/subfolder/en.ftl";
        String config = "Some email content";
        String configCustom = "Some custom email content";

        templateService.onRefresh(emailPath, config);
        templateService.onRefresh(customEmailPath, configCustom);

        assertThat(templateService.getEmailTemplate("XM", "activation/subfolder", "en")).isEqualTo(config);
        assertThat(templateService.getEmailTemplate("XM", "register/subfolder", "en")).isEqualTo(configCustom);
        assertThat(templateService.getEmailTemplate("XM", "activation/subfolder", "")).isEqualTo(config);
        assertThat(getContentFromTemplateLoader("XM/activation/subfolder/en")).isEqualTo(config);
        assertThat(getContentFromTemplateLoader("XM/register/subfolder/en")).isEqualTo(configCustom);
    }

    @Test
    public void testEmailTemplateUpdateConfig() {
        String emailPath = "/config/tenants/XM/communication/emails/activation/subfolder/en.ftl";
        String customEmailPath = "/config/tenants/XM/communication/custom-emails/register/subfolder/en.ftl";
        String config = "Some email content";
        String configCustom = "Some custom email content";
        String newConfig = "Some new email content";
        String newConfigCustom = "Some new custom email content";

        templateService.onRefresh(emailPath, config);
        templateService.onRefresh(customEmailPath, configCustom);
        templateService.onRefresh(emailPath, newConfig);
        templateService.onRefresh(customEmailPath, newConfigCustom);

        assertThat(templateService.getEmailTemplate("XM", "activation/subfolder", "en")).isEqualTo(newConfig);
        assertThat(templateService.getEmailTemplate("XM", "register/subfolder", "en")).isEqualTo(newConfigCustom);
    }

    @Test
    public void testEmailTemplateDeleteConfig() {
        String customEmailPath = "/config/tenants/XM/communication/custom-emails/register/subfolder/en.ftl";
        String configCustom = "Some custom email content";

        templateService.onRefresh(customEmailPath, configCustom);
        templateService.onRefresh(customEmailPath, "");

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            templateService.getEmailTemplate("XM", "register/subfolder", "en"));
        String expectedMessage = "Email template was not found";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testEmailTemplateLoaderCrossUpdateConfig() {
        String emailPath = "/config/tenants/XM/communication/emails/activation/subfolder/en.ftl";
        String customEmailPath = "/config/tenants/XM/communication/custom-emails/activation/subfolder/en.ftl";
        String config = "Some email content";
        String configCustom = "Some custom email content";

        templateService.onRefresh(emailPath, config);
        templateService.onRefresh(customEmailPath, configCustom);
        templateService.onRefresh(emailPath, config);
        assertThat(getContentFromTemplateLoader("XM/activation/subfolder/en")).isEqualTo(configCustom);

        templateService.onRefresh(customEmailPath, "");
        assertThat(getContentFromTemplateLoader("XM/activation/subfolder/en")).isEqualTo(config);

        templateService.onRefresh(customEmailPath, configCustom);
        templateService.onRefresh(emailPath, "");
        assertThat(getContentFromTemplateLoader("XM/activation/subfolder/en")).isEqualTo(configCustom);

        templateService.onRefresh(emailPath, "");
        templateService.onRefresh(customEmailPath, "");
        assertThrows(NullPointerException.class, () -> getContentFromTemplateLoader("XM/activation/subfolder/en"));

    }

    @Test
    public void testGetEmailTemplateByKeyDefault() {
        String emailPath = "/config/tenants/RESINTTEST/communication/emails/activation/secondTemplateKey/en.ftl";
        String config = "Some email content";

        String emailSpecConfig = loadFile("config/specs/email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecConfig);
        templateService.onRefresh(emailPath, config);

        String emailTemplate = templateService.getEmailTemplateByKey(TenantKey.valueOf(TENANT_NAME), "secondTemplateKey", "en");

        assertThat(emailTemplate).isEqualTo(config);
    }

    @Test(expected = EntityNotFoundException.class)
    public void testGetEmailTemplateByKeyWithoutPath() {
        String emailPath = "/config/tenants/RESINTTEST/communication/emails/activation/thirdTemplateKey/en.ftl";
        String config = "Some email content";

        String emailSpecConfig = loadFile("config/specs/email-spec.yml");
        emailSpecService.onRefresh(EMAIL_SPECIFICATION_PATH, emailSpecConfig);
        templateService.onRefresh(emailPath, config);

        templateService.getEmailTemplateByKey(TenantKey.valueOf(TENANT_NAME), "thirdTemplateKey", "en");
    }

    @Test
    public void testGetEmailTemplateByKeyWithoutSpec() {
        String emailPath = "/config/tenants/RESINTTEST/communication/emails/noSpecTemplateName/en.ftl";
        String config = "Some email content";

        templateService.onRefresh(emailPath, config);

        String emailTemplate = templateService.getEmailTemplateByKey(TenantKey.valueOf(TENANT_NAME), "noSpecTemplateName", "en");

        assertThat(emailTemplate).isEqualTo(config);
    }

    @SneakyThrows
    private String getContentFromTemplateLoader(String templateKey) {
        Object templateSource = templateLoader.findTemplateSource(templateKey);
        return IOUtils.toString(templateLoader.getReader(templateSource, ""));
    }
}
