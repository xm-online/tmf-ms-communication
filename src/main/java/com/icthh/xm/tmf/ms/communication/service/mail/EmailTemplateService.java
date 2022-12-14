package com.icthh.xm.tmf.ms.communication.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.CustomEmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.CustomEmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.mapper.TemplateDetailsMapper;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.icthh.xm.tmf.ms.communication.config.Constants.CONFIG_PATH_TEMPLATE;
import static com.icthh.xm.tmf.ms.communication.config.Constants.CUSTOM_EMAIL_PATH;
import static com.icthh.xm.tmf.ms.communication.config.Constants.CUSTOM_EMAIL_SPEC;
import static java.util.Optional.ofNullable;
import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_LANGUAGE;
import static java.util.Optional.of;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final freemarker.template.Configuration freeMarkerConfiguration;
    private final EmailSpecService emailSpecService;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final CustomEmailSpecService customEmailSpecService;
    private final CommonConfigRepository  commonConfigRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
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

    @SneakyThrows
    public void updateTemplate(String templateKey, String langKey, UpdateTemplateRequest updateTemplateRequest) {
        EmailTemplateSpec emailTemplateSpec = emailSpecService.getEmailTemplateSpecByKey(templateKey);

        String tenantKey = tenantContextHolder.getTenantKey();
        String configPath = String.format(CONFIG_PATH_TEMPLATE, tenantKey);

        String subject = ofNullable(emailTemplateSpec.getSubjectTemplate().get(langKey)).orElse(StringUtils.EMPTY);
        if (!subject.equals(updateTemplateRequest.getTemplateSubject())) {
            updateTemplateSpecSubject(templateKey, langKey, updateTemplateRequest.getTemplateSubject(), configPath);
        }

        updateTemplateContent(emailTemplateSpec.getTemplatePath(), langKey, updateTemplateRequest.getContent(), configPath);
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
            .map(it -> it.getOrDefault(langKey, it.get(DEFAULT_LANGUAGE)))
            .orElseThrow(() -> new EntityNotFoundException("Email subject was not found"));
    }

    @SneakyThrows
    private void updateTemplateSpecSubject(String templateKey, String langKey, String subject, String configPath) {
        CustomEmailTemplateSpec customEmailTemplateSpec = new CustomEmailTemplateSpec();
        customEmailTemplateSpec.setTemplateKey(templateKey);
        customEmailTemplateSpec.setSubjectTemplate(Map.of(langKey, subject));
        CustomEmailSpec updatedCustomEmailSpec = customEmailSpecService.updateCustomEmailSpec(customEmailTemplateSpec);

        String emailsSpecYml = yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatedCustomEmailSpec);
        Configuration configuration = Configuration.of().path(configPath + CUSTOM_EMAIL_SPEC).content(emailsSpecYml).build();
        updateConfig(configuration);
    }

    private void updateTemplateContent(String templatePath, String langKey, String content, String configPath) {
        String templateFileName = String.format("/%s.ftl", langKey);
        Configuration configuration = Configuration.of().path(configPath + CUSTOM_EMAIL_PATH + templatePath + templateFileName).content(content).build();
        updateConfig(configuration);
    }

    private void updateConfig(Configuration configuration) {
        String oldConfigHash = configToSha1Hex(getOldConfig(configuration.getPath()));

        if (DigestUtils.sha1Hex(configuration.getContent()).equals(oldConfigHash)) {
            log.info("Email template configuration not changed");
            return;
        }
        commonConfigRepository.updateConfigFullPath(configuration, oldConfigHash);
    }

    private String configToSha1Hex(Optional<Configuration> configuration) {
        return configuration
            .map(Configuration::getContent)
            .map(DigestUtils::sha1Hex)
            .orElse(null);
    }

    private Optional<Configuration> getOldConfig(String configPath) {
        List<String> paths = Collections.singletonList(configPath);
        Map<String, Configuration> configs = commonConfigRepository.getConfig(null, paths);
        configs = configs == null ? new HashMap<>() : configs;

        return ofNullable(configs.get(configPath));
    }
}
