package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
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

    private static String UPDATED_TEMPLATE_NAME = "updated template name";
    private static String UPDATED_SUBJECT_NAME = "updated subject name";

    private TenantContextHolder tenantContextHolder;

    private EmailSpecService emailSpecService;

    private EmailTemplateService subject;

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @MockBean
    private SmppService smppService;

    @MockBean
    private RestTemplate restTemplate;
    @Autowired
    private freemarker.template.Configuration freeMarkerConfiguration;


    @Before
    public void init() {
        tenantContextHolder = mock(TenantContextHolder.class);
        mockTenant("TEST");

        customEmailSpecService = new CustomEmailSpecService(applicationProperties);
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
        UpdateTemplateRequest updateTemplateRequest = createUpdateRequestTemplate();

        subject.updateTemplate("firstTemplateKey", updateTemplateRequest);
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
}
