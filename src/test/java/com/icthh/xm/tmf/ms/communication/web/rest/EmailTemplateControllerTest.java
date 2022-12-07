package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.service.mail.EmailTemplateService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailTemplateControllerTest {

    private static final String DEFAULT_RENDERED_RESPONSE = "xm@test.com";
    private static final String DEFAULT_CONTENT = "${subject}@${domainName}.com";
    private static final String API_BASE = "/api/templates";

    private MockMvc mockMvc;

    @Autowired
    private EmailTemplateController subject;

    @MockBean
    private EmailTemplateService emailTemplateService;

    @MockBean
    private EmailSpecService emailSpecService;

    @MockBean
    private SmppService smppService;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(subject).build();
    }

    @Test
    @SneakyThrows
    public void renderEmailContent() {
        RenderTemplateRequest renderTemplateRequest = createEmailTemplateDto();
        RenderTemplateResponse renderTemplateResponse = createRenderTemplateResponse();

        when(emailTemplateService.renderEmailContent(eq(renderTemplateRequest))).thenReturn(renderTemplateResponse);

        mockMvc.perform(post(API_BASE + "/render")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(renderTemplateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(renderTemplateResponse.getContent()));

        verify(emailTemplateService).renderEmailContent(eq(renderTemplateRequest));
        verifyNoMoreInteractions(emailTemplateService);
    }

    @Test
    @SneakyThrows
    public void renderEmailContentThrowBadRequestWhenContentMissed() {
        RenderTemplateRequest renderTemplateRequest = new RenderTemplateRequest();

        mockMvc.perform(post(API_BASE + "/render")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(renderTemplateRequest)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(emailTemplateService);
    }

    @Test
    @SneakyThrows
    public void getEmailSpec() {
        List<EmailTemplateSpec> emailTemplateSpecList = List.of(
            new EmailTemplateSpec("firstKey", "Name 1", "Subject 1", "firstKey.ftl", "{}", "{}", "{}"),
            new EmailTemplateSpec("secondKey", "Name 2", "Subject 2", "secondKey.ftl", "{}", "{}", "{}")
        );
        EmailSpec emailSpec = new EmailSpec();
        emailSpec.setEmails(emailTemplateSpecList);

        when(emailSpecService.getEmailSpec()).thenReturn(emailSpec);

        mockMvc.perform(get(API_BASE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(emailTemplateSpecList.size())))
            .andExpect(jsonPath("$.[*].templateKey").value(containsInAnyOrder("firstKey", "secondKey")))
            .andExpect(jsonPath("$.[*].name").value(containsInAnyOrder("Name 1", "Name 2")))
            .andExpect(jsonPath("$.[*].subjectTemplate").value(containsInAnyOrder("Subject 1", "Subject 2")))
            .andExpect(jsonPath("$.[*].templatePath").value(containsInAnyOrder("firstKey.ftl", "secondKey.ftl")))
            .andExpect(jsonPath("$.[*].contextExample").value(containsInAnyOrder(two("{}"))))
            .andExpect(jsonPath("$.[*].contextSpec").value(containsInAnyOrder(two("{}"))))
            .andExpect(jsonPath("$.[*].contextForm").value(containsInAnyOrder(two("{}"))));

        verify(emailSpecService).getEmailSpec();
        verifyNoMoreInteractions(emailSpecService);
    }

    private RenderTemplateRequest createEmailTemplateDto() {
        RenderTemplateRequest renderTemplateRequest = new RenderTemplateRequest();
        renderTemplateRequest.setContent(DEFAULT_CONTENT);
        renderTemplateRequest.setModel(Map.of("subject", "xm", "domainName", "test"));
        return renderTemplateRequest;
    }

    private RenderTemplateResponse createRenderTemplateResponse(){
        RenderTemplateResponse renderTemplateResponse = new RenderTemplateResponse();
        renderTemplateResponse.setContent(DEFAULT_RENDERED_RESPONSE);
        return  renderTemplateResponse;
    }

    private static Object[] two(Object single) {
        return new Object[]{single, single};
    }
}
