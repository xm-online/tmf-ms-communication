package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.mapper.TemplateDetailsMapper;
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
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Optional.of;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final Configuration freeMarkerConfiguration;
    private final EmailSpecService emailSpecService;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final TenantContextHolder tenantContextHolder;

    private final TemplateDetailsMapper templateDetailsMapper;

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

    public TemplateDetails getTemplateDetailsByKey(String templateKey, String langKey) {
        List<String> langs = emailSpecService.getEmailSpec().getLangs();
        EmailTemplateSpec emailTemplateSpec = emailSpecService.getEmailTemplateSpecByKey(templateKey);
        String subjectTemplate = getSubjectTemplateByLang(emailTemplateSpec, langKey);
        String templatePath = emailTemplateSpec.getTemplatePath();
        String tenantKey = tenantContextHolder.getTenantKey();
        String templateContent = tenantEmailTemplateService.getEmailTemplate(tenantKey, templatePath, langKey);

        return createTemplateDetails(templateContent, emailTemplateSpec, langs, subjectTemplate);
    }

    private TemplateDetails createTemplateDetails(String templateContent, EmailTemplateSpec emailTemplateSpec,
                                                  List<String> langs, String subjectTemplate) {
        TemplateDetails templateDetails = templateDetailsMapper.emailTemplateToDetails(emailTemplateSpec);
        templateDetails.setContent(templateContent);
        templateDetails.setSubjectTemplate(subjectTemplate);
        templateDetails.setLangs(langs);
        return templateDetails;
    }

    private String getSubjectTemplateByLang(EmailTemplateSpec emailTemplateSpec, String langKey) {
        return of(emailTemplateSpec)
            .map(EmailTemplateSpec::getSubjectTemplate)
            .map(it -> it.get(langKey))
            .orElseThrow(() -> new EntityNotFoundException(format("Email template was not found with language: %s", langKey)));
    }
}
