package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.dto.EmailTemplateDto;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.messaging.handler.CustomCommunicationMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.service.mail.EmailTemplateService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiController;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.CustomExceptionTranslator;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailTemplateControllerTest {

    private static final String DEFAULT_RENDERED_RESPONSE = "htmlRepsponse";
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
    public void renderEmailContentToHtml() {
        EmailTemplateDto emailTemplateDto = createEmailTemplateDto();
        when(emailTemplateService.renderEmailContent(eq(emailTemplateDto))).thenReturn(DEFAULT_RENDERED_RESPONSE);

        mockMvc.perform(post(API_BASE + "/render")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(emailTemplateDto)))
            .andExpect(status().isOk());

        verify(emailTemplateService).renderEmailContent(eq(emailTemplateDto));
        verifyNoMoreInteractions(emailTemplateService);
    }

    @Test
    @SneakyThrows
    public void renderEmailContentToHtmlThrowBadRequestWhenContentMissed() {
        EmailTemplateDto emailTemplateDto = new EmailTemplateDto();
        when(emailTemplateService.renderEmailContent(eq(emailTemplateDto))).thenReturn(DEFAULT_RENDERED_RESPONSE);

        mockMvc.perform(post(API_BASE + "/render")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(emailTemplateDto)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(emailTemplateService);
    }

    @Test
    @SneakyThrows
    public void renderEmailContentToHtmlReturnNullWhenTemplateNotRendered() {
        EmailTemplateDto emailTemplateDto = createEmailTemplateDto();
        when(emailTemplateService.renderEmailContent(eq(emailTemplateDto))).thenReturn(null);

        mockMvc.perform(post(API_BASE + "/render")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(emailTemplateDto)))
            .andExpect(status().isOk());

        verify(emailTemplateService).renderEmailContent(eq(emailTemplateDto));
        verifyNoMoreInteractions(emailTemplateService);
    }

    private EmailTemplateDto createEmailTemplateDto() {
        EmailTemplateDto emailTemplateDto = new EmailTemplateDto();
        emailTemplateDto.setContent("");
        emailTemplateDto.setModel(Map.of());
        return emailTemplateDto;
    }
}
