package com.icthh.xm.tmf.ms.communication.service.mail;

import static java.util.Locale.ENGLISH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SuppressWarnings("unused")
@ActiveProfiles(profiles = "non-async")
public class MailServiceIntTest {

    private static final String MAIL_SETTINGS = "mailSettings";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String EMAIL = "email";
    private static final String FROM = "from";
    private static final String RID = "rid";
    public static final String TENANT_NAME = "RESINTTEST";

    @SpyBean
    private MailService mailService;

    @Autowired
    private TenantEmailTemplateService templateService;

    private JavaMailSender javaMailSender = mock(JavaMailSender.class);

    @Spy
    private MailProviderService mailProviderService = new MailProviderService(javaMailSender);

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @SneakyThrows
    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_NAME);
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));
    }

    @Test
    public void testComplexTemplateEmail() throws InterruptedException {

        String mainPath = "/config/tenants/" + TENANT_NAME + "/communication/emails/otp/" + TEMPLATE_NAME + "/en.ftl";
        String basePath = "/config/tenants/" + TENANT_NAME + "/communication/emails/otp/" + TEMPLATE_NAME + "/en.ftl";
        String body = "<#import \"/" + TENANT_NAME + "/en/" + TEMPLATE_NAME + "-BASE\" as main>OTHER_<@main.body>_CUSTOM_</@main.body>";
        String base = "<#macro body>BASE_START<#nested>BASE_END</#macro>";
        templateService.onRefresh(mainPath, body);
        templateService.onRefresh(basePath, base);
        mailService.sendEmailFromTemplate(TenantKey.valueOf(TENANT_NAME), ENGLISH, TEMPLATE_NAME, SUBJECT, EMAIL, Map.of(
                "variable1", "value1",
                "variable2", "value2"
        ), RID, FROM);

        verify(mailService).sendEmail(any(), any(), eq("OTHER_BASE_START_CUSTOM_BASE_END"), any(), any(), any());
    }

}