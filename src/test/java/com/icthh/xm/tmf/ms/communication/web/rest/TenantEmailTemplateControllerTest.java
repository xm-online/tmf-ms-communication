package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.tmf.ms.communication.domain.dto.EmailTemplateDto;
import com.icthh.xm.tmf.ms.communication.service.EmailTemplateService;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = TenantEmailTemplateController.class)
public class TenantEmailTemplateControllerTest {

    private static final String DEFAULT_RENDERED_RESPONSE = "htmlRepsponse";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailTemplateService emailTemplateService;


    @Test
    @SneakyThrows
    public void renderEmailContentToHtml() {
        EmailTemplateDto emailTemplateDto = createEmailTemplateDto();
        when(emailTemplateService.renderEmailContent(eq(emailTemplateDto))).thenReturn(DEFAULT_RENDERED_RESPONSE);

        mockMvc.perform(post("/templates/render").contentType(MediaType.APPLICATION_JSON));

        verify(emailTemplateService).renderEmailContent(eq(emailTemplateDto));
        verifyNoMoreInteractions(emailTemplateService);
    }

    private EmailTemplateDto createEmailTemplateDto() {
        EmailTemplateDto emailTemplateDto = new EmailTemplateDto();
        emailTemplateDto.setContent("");
        emailTemplateDto.setModel(Map.of());
        return null;
    }
}
