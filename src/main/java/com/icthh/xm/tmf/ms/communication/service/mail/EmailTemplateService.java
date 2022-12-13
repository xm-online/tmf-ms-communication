package com.icthh.xm.tmf.ms.communication.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.CustomEmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.UUID;

import static com.icthh.xm.tmf.ms.communication.config.Constants.CONFIG_PATH_TEMPLATE;
import static com.icthh.xm.tmf.ms.communication.config.Constants.CUSTOM_EMAIL_PATH;
import static com.icthh.xm.tmf.ms.communication.config.Constants.CUSTOM_EMAIL_SPEC;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final freemarker.template.Configuration freeMarkerConfiguration;
    private final EmailSpecService emailSpecService;
    private final CustomEmailSpecService customEmailSpecService;
    private final CommonConfigRepository  commonConfigRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

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
    public void updateTemplate(String templateKey, String langKey, UpdateTemplateRequest updateTemplateRequest) {
        EmailTemplateSpec emailTemplateSpec = emailSpecService.getEmailTemplateSpecByTemplateKey(templateKey);

        CustomEmailTemplateSpec customEmailTemplateSpec = new CustomEmailTemplateSpec(templateKey, updateTemplateRequest.getTemplateSubject());
        CustomEmailSpec updatedCustomEmailSpec = customEmailSpecService.updateCustomEmailSpec(customEmailTemplateSpec);

        String tenantKey = tenantContextHolder.getTenantKey();
        String configPath = String.format(CONFIG_PATH_TEMPLATE, tenantKey);

        String emailsSpecYml = yamlMapper.writeValueAsString(updatedCustomEmailSpec);
        Configuration configuration = Configuration.of().path(configPath + CUSTOM_EMAIL_SPEC).content(emailsSpecYml).build();
        commonConfigRepository.updateConfigFullPath(configuration, DigestUtils.sha1Hex(emailsSpecYml));

        String templatePath = emailTemplateSpec.getTemplatePath();
        String templateFileName = String.format("/%s.ftl", langKey);
        configuration = Configuration.of().path(configPath + CUSTOM_EMAIL_PATH + templatePath + templateFileName).content(updateTemplateRequest.getContent()).build();
        commonConfigRepository.updateConfigFullPath(configuration, DigestUtils.sha1Hex(updateTemplateRequest.getContent()));
    }

}
