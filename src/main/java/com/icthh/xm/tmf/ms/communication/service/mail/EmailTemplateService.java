package com.icthh.xm.tmf.ms.communication.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.CustomEmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final freemarker.template.Configuration freeMarkerConfiguration;
    private final EmailSpecService emailSpecService;
    private final CustomEmailSpecService customEmailSpecService;
    private final TenantConfigRepository tenantConfigRepository;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

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
    @SneakyThrows
    public void updateTemplate(String templateKey, UpdateTemplateRequest updateTemplateRequest) {
        EmailSpec emailSpec = emailSpecService.getEmailSpec();

        EmailTemplateSpec emailTemplateSpec = emailSpec.getEmails()
                                                        .stream()
                                                        .filter((spec) -> spec.getTemplateKey().equals(templateKey))
                                                        .findFirst()
                                                        .orElseThrow(() -> new EntityNotFoundException("Email template specification not found"));

        emailTemplateSpec.setName(updateTemplateRequest.getTemplateName());
        emailTemplateSpec.setSubjectTemplate(updateTemplateRequest.getTemplateSubject());

        List<Configuration> configurations = new ArrayList<>();
        String emailsSpecYml = mapper.writeValueAsString(emailSpec);
        configurations.add(Configuration.of().path(applicationProperties.getEmailSpecificationPathPattern()).content(emailsSpecYml).build());

        String templatePath = emailTemplateSpec.getTemplatePath();
        configurations.add(Configuration.of().path("/config/tenants/{tenantKey}/communication/custom-emails/" + templatePath).content(updateTemplateRequest.getContent()).build());

        //TODO: just for testing. create proper method for client update config
        tenantConfigRepository.createConfigsFullPath("tenantKey", configurations);
    }
}
