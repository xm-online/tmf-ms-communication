package com.icthh.xm.tmf.ms.communication.service.mail;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.CommunicationTenantConfigService;
import com.icthh.xm.tmf.ms.communication.config.CommunicationTenantConfigService.CommunicationTenantConfig.MailSetting;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.mail.internet.MimeMessage;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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

    @Autowired
    private MailService mailService;

    @Autowired
    private TenantEmailTemplateService templateService;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private MailProviderService mailProviderService;

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
    }

    @Test
    public void testComplexTemplateEmail() {
        String mainPath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "/en.ftl";
        String basePath = "/config/tenants/" + TENANT_NAME + "/communication/emails/" + TEMPLATE_NAME + "-BASE/en.ftl";
        String body = "<#import \"/" + TENANT_NAME + "/" + TEMPLATE_NAME + "-BASE/en\" as main>OTHER_<@main.body>_CUSTOM_</@main.body>";
        String base = "<#macro body>BASE_START<#nested>BASE_END</#macro>";
        templateService.onRefresh(mainPath, body);
        templateService.onRefresh(basePath, base);
        MailService spiedMailService = spy(mailService);
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
            captor.capture(), // to
            captor.capture(), // subject
            eq("OTHER_BASE_START_CUSTOM_BASE_END"),
            captor.capture(), // from
            any(),
            any()
        );

        List<String> allValues = captor.getAllValues();
        assertThat(allValues).containsExactly(EMAIL, "Subject with value1", "test@communication.com");
    }
}
