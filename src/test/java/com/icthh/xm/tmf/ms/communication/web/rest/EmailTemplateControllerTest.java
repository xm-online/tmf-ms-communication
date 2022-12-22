package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.service.mail.EmailTemplateService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_LANGUAGE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class EmailTemplateControllerTest {

    private static final String DEFAULT_RENDERED_RESPONSE = "xm@test.com";
    private static final String DEFAULT_CONTENT = "${subject}@${domainName}.com";
    private static final String API_BASE = "/api/templates";
    private static final String DEFAULT_TEMPLATE_KEY = "templateKey";
    private static final String TEMPLATE_KEY = "templateKey1";

    private MockMvc mockMvc;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private Validator validator;

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
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
        MockitoAnnotations.openMocks(this);

        this.mockMvc = MockMvcBuilders.standaloneSetup(subject)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
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
        Map<String, String> firstSubject = Map.of(DEFAULT_LANGUAGE,"Subject 1");
        Map<String, String> secondSubject = Map.of(DEFAULT_LANGUAGE,"Subject 2");
        List<EmailTemplateSpec> emailTemplateSpecList = List.of(
            new EmailTemplateSpec("firstKey", "Name 1", firstSubject, "firstKey.ftl", "{}", "{}", "{}"),
            new EmailTemplateSpec("secondKey", "Name 2", secondSubject, "secondKey.ftl", "{}", "{}", "{}")
        );
        EmailSpec emailSpec = new EmailSpec();
        emailSpec.setEmails(emailTemplateSpecList);

        when(emailSpecService.getEmailSpec()).thenReturn(emailSpec);

        mockMvc.perform(get(API_BASE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(emailTemplateSpecList.size())))
            .andExpect(jsonPath("$.[*].templateKey").value(containsInAnyOrder("firstKey", "secondKey")))
            .andExpect(jsonPath("$.[*].name").value(containsInAnyOrder("Name 1", "Name 2")))
            .andExpect(jsonPath("$.[*].subjectTemplate.en").value(containsInAnyOrder("Subject 1", "Subject 2")))
            .andExpect(jsonPath("$.[*].templatePath").value(containsInAnyOrder("firstKey.ftl", "secondKey.ftl")))
            .andExpect(jsonPath("$.[*].contextExample").value(containsInAnyOrder(two("{}"))))
            .andExpect(jsonPath("$.[*].contextSpec").value(containsInAnyOrder(two("{}"))))
            .andExpect(jsonPath("$.[*].contextForm").value(containsInAnyOrder(two("{}"))));

        verify(emailSpecService).getEmailSpec();
        verifyNoMoreInteractions(emailSpecService);
    }

    @Test
    @SneakyThrows
    public void getTemplateDetailsByKey() {
        TemplateDetails templateDetails = createTemplateDetails();

        when(emailTemplateService.getTemplateDetailsByKey(eq(DEFAULT_TEMPLATE_KEY), eq(DEFAULT_LANGUAGE))).thenReturn(templateDetails);

        mockMvc.perform(get(API_BASE + "/" + DEFAULT_TEMPLATE_KEY + "/" + DEFAULT_LANGUAGE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjectTemplate").value(templateDetails.getSubjectTemplate()))
            .andExpect(jsonPath("$.content").value(templateDetails.getContent()))
            .andExpect(jsonPath("$.contextExample").value(templateDetails.getContextExample()))
            .andExpect(jsonPath("$.contextSpec").value(templateDetails.getContextSpec()))
            .andExpect(jsonPath("$.contextForm").value(templateDetails.getContextForm()));
    }

    @Test
    @SneakyThrows
    public void testUpdateTemplate() {
        UpdateTemplateRequest updateTemplateRequest = createUpdateRequestTemplate();

        mockMvc.perform(put(API_BASE + "/" + TEMPLATE_KEY + "/" + DEFAULT_LANGUAGE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(updateTemplateRequest)))
            .andExpect(status().isOk());

        verify(emailTemplateService).updateTemplate(eq(TEMPLATE_KEY), eq(DEFAULT_LANGUAGE), refEq(updateTemplateRequest));
        verifyNoMoreInteractions(emailTemplateService);
    }

    private TemplateDetails createTemplateDetails() {
        TemplateDetails templateDetails = new TemplateDetails();
        templateDetails.setContent(DEFAULT_CONTENT);
        templateDetails.setSubjectTemplate("Subject 1");
        templateDetails.setContextSpec("{}");
        templateDetails.setContextForm("{}");
        templateDetails.setContextExample("{}");
        templateDetails.setLangs(List.of("en", "uk"));
        return templateDetails;
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

    private UpdateTemplateRequest createUpdateRequestTemplate() {
        UpdateTemplateRequest updateTemplateRequest = new UpdateTemplateRequest();
        updateTemplateRequest.setSubjectTemplate("template subject");
        updateTemplateRequest.setContent("some content");

        return updateTemplateRequest;
    }
}
