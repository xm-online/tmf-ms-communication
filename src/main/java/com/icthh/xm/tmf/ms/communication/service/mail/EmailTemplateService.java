package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.UUID;

import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_LANGUAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final Configuration freeMarkerConfiguration;
    private final EmailSpecService emailSpecService;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final TenantContextHolder tenantContextHolder;

    @SneakyThrows
    public RenderTemplateResponse renderEmailContent(RenderTemplateRequest renderTemplateRequest) {
        try {
            Template mailTemplate = new Template(UUID.randomUUID().toString(), renderTemplateRequest.getContent(), freeMarkerConfiguration);
            String renderedContent = FreeMarkerTemplateUtils.processTemplateIntoString(mailTemplate, renderTemplateRequest.getModel());
            RenderTemplateResponse renderTemplateResponse = new RenderTemplateResponse();
            renderTemplateResponse.setContent(renderedContent);
            return renderTemplateResponse;
        } catch (TemplateException e) {
            log.error("Template could not be rendered with content: {} and model: {}.", renderTemplateRequest.getContent(),
                renderTemplateRequest.getModel(), e);
            throw new RenderTemplateException(e.getMessageWithoutStackTop(), renderTemplateRequest.getContent(), renderTemplateRequest.getModel());
        } catch (IOException e) {
            log.error("Template could not be rendered with content: {} and model: {}.", renderTemplateRequest.getContent(),
                renderTemplateRequest.getModel(), e);
            throw new RenderTemplateException(e.getMessage(), renderTemplateRequest.getContent(), renderTemplateRequest.getModel());
        }
    }

    public TemplateDetails getTemplateDetailsByKey(String templateKey) {
        EmailTemplateSpec emailTemplateSpec = getEmailTemplateSpecByKey(templateKey);
        String templatePath = emailTemplateSpec.getTemplatePath();
        String tenantKey = tenantContextHolder.getTenantKey();
        String templateContent = tenantEmailTemplateService.getEmailTemplate(tenantKey, templatePath, DEFAULT_LANGUAGE);
        return createTemplateDetails(templateContent, emailTemplateSpec);
    }

    private EmailTemplateSpec getEmailTemplateSpecByKey(String templateKey) {
        return emailSpecService.getEmailSpec().getEmails()
            .stream()
            .filter((spec) -> spec.getTemplateKey().equals(templateKey))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Email template specification not found"));
    }

    private TemplateDetails createTemplateDetails(String templateContent, EmailTemplateSpec emailTemplateSpec) {
        TemplateDetails templateDetails = new TemplateDetails();
        templateDetails.setContent(templateContent);
        templateDetails.setName(emailTemplateSpec.getName());
        templateDetails.setSubject(emailTemplateSpec.getSubjectTemplate());
        templateDetails.setEmailSpec(emailTemplateSpec.getContextSpec());
        templateDetails.setEmailForm(emailTemplateSpec.getContextForm());
        templateDetails.setEmailData(emailTemplateSpec.getContextExample());
        return templateDetails;
    }
}
