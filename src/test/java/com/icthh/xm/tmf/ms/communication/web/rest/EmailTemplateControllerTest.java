package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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
}
