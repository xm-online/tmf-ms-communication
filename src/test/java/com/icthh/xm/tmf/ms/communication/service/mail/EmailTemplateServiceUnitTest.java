package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = MessageCollectorAutoConfiguration.class)
@SpringBootTest(classes = {CommunicationApp.class, SecurityBeanOverrideConfiguration.class})
public class EmailTemplateServiceUnitTest {

    @Autowired
    private EmailTemplateService subject;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @MockBean
    private SmppService smppService;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
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
}
