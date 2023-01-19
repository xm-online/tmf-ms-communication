package com.icthh.xm.tmf.ms.communication.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
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
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
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
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

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
    private final ObjectMapper objectMapper;
    private final TemplateDetailsMapper templateDetailsMapper;
    private final StringTemplateLoader templateLoader;
    private final MultiTenantLangStringTemplateLoaderService multiTenantLangStringTemplateLoaderService;

    @SneakyThrows
    public RenderTemplateResponse renderEmailContent(RenderTemplateRequest renderTemplateRequest) {
        String tenantKey = tenantContextHolder.getTenantKey();
        String renderedContent = processEmailTemplate(tenantKey, renderTemplateRequest.getContent(), renderTemplateRequest.getModel(), renderTemplateRequest.getLang(), UUID.randomUUID().toString());
        RenderTemplateResponse renderTemplateResponse = new RenderTemplateResponse();
        renderTemplateResponse.setContent(renderedContent);
        return renderTemplateResponse;
    }

    public String processEmailTemplate(String tenantKey, String content, Map<String, Object> objectModel, String lang, String templatePath) {
        try {
            freemarker.template.Configuration configuration = (freemarker.template.Configuration) freeMarkerConfiguration.clone();
            StringTemplateLoader templateLoaderByTenantAndLang = multiTenantLangStringTemplateLoaderService.getTemplateLoader(tenantKey, lang);
            MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(
                new TemplateLoader[]{templateLoaderByTenantAndLang, templateLoader}
            );
            configuration.setTemplateLoader(multiTemplateLoader);

            Template mailTemplate = new Template(templatePath, content, configuration);

            return FreeMarkerTemplateUtils.processTemplateIntoString(mailTemplate, objectModel);
        } catch (TemplateException e) {
            log.error("Template could not be rendered with content: {} and model: {} for language: {}.", content,
                objectModel, lang, e);
            throw new RenderTemplateException(e.getMessageWithoutStackTop(), content, objectModel, lang);
        } catch (IOException e) {
            log.error("Template could not be rendered with content: {} and model: {} for language: {}.", content,
                objectModel, lang, e);
            throw new RenderTemplateException(e.getMessage(), content, objectModel, lang);
        }
    }

    public TemplateDetails getTemplateDetailsByKey(String templateKey, String langKey) {
        List<String> langs = emailSpecService.getEmailSpec().getLangs();
        EmailTemplateSpec emailTemplateSpec = emailSpecService.getEmailTemplateSpecByKey(templateKey);
        String subjectTemplate = getSubjectTemplateByLang(emailTemplateSpec, langKey);
        String emailFrom = getEmailFromByLang(emailTemplateSpec, langKey);
        String templatePath = emailTemplateSpec.getTemplatePath();
        String tenantKey = tenantContextHolder.getTenantKey();
        String templateContent = tenantEmailTemplateService.getEmailTemplate(tenantKey, templatePath, langKey);

        return createTemplateDetails(templateContent, emailTemplateSpec, langs, subjectTemplate, langKey, emailFrom);
    }

    @SneakyThrows
    public void updateTemplate(String templateKey, String langKey, UpdateTemplateRequest updateTemplateRequest) {
        EmailTemplateSpec emailTemplateSpec = emailSpecService.getEmailTemplateSpecByKey(templateKey);

        String tenantKey = tenantContextHolder.getTenantKey();
        String configPath = String.format(CONFIG_PATH_TEMPLATE, tenantKey);

        String subject = ofNullable(emailTemplateSpec.getSubjectTemplate().get(langKey)).orElse(StringUtils.EMPTY);
        String from = ofNullable(emailTemplateSpec.getEmailFrom().get(langKey)).orElse(StringUtils.EMPTY);
        if (!subject.equals(updateTemplateRequest.getSubjectTemplate()) || !from.equals(updateTemplateRequest.getEmailFrom())) {
            updateTemplateSpecProps(templateKey, langKey, updateTemplateRequest.getSubjectTemplate(), updateTemplateRequest.getEmailFrom(), configPath);
        }

        updateTemplateContent(emailTemplateSpec.getTemplatePath(), langKey, updateTemplateRequest.getContent(), configPath);
    }

    private TemplateDetails createTemplateDetails(String templateContent, EmailTemplateSpec emailTemplateSpec,
                                                  List<String> langs, String subjectTemplate, String langKey, String emailFrom) {
        TemplateDetails templateDetails = templateDetailsMapper.emailTemplateToDetails(emailTemplateSpec);
        templateDetails.setContent(templateContent);
        templateDetails.setSubjectTemplate(subjectTemplate);
        templateDetails.setEmailFrom(emailFrom);
        templateDetails.setLangs(langs);

        List<String> dependsOnTemplateKeys = emailTemplateSpec.getDependsOnTemplateKeys();
        if (dependsOnTemplateKeys != null) {
            dependsOnTemplateKeys.forEach(it -> {
                TemplateDetails parentTemplateDetails = getTemplateDetailsByKey(it, langKey);
                templateDetails.setContextExample(mergeJsons(parentTemplateDetails.getContextExample(), templateDetails.getContextExample()));
                templateDetails.setContextSpec(mergeJsons(parentTemplateDetails.getContextSpec(), templateDetails.getContextSpec()));
                templateDetails.setContextForm(mergeJsons(parentTemplateDetails.getContextForm(), templateDetails.getContextForm()));
            });
        }

        return templateDetails;
    }

    private String getSubjectTemplateByLang(EmailTemplateSpec emailTemplateSpec, String langKey) {
        return getTemplatePropertyByLang(emailTemplateSpec, langKey, EmailTemplateSpec::getSubjectTemplate)
            .orElseThrow(() -> new EntityNotFoundException("Email subject was not found"));
    }

    private String getEmailFromByLang(EmailTemplateSpec emailTemplateSpec, String langKey) {
        return getTemplatePropertyByLang(emailTemplateSpec, langKey, EmailTemplateSpec::getEmailFrom)
            .orElseThrow(() -> new EntityNotFoundException("Email from was not found"));
    }

    private Optional<String> getTemplatePropertyByLang(EmailTemplateSpec emailTemplateSpec, String langKey, Function<EmailTemplateSpec, Map<String, String>> fieldMapper) {
        return of(emailTemplateSpec)
            .map(fieldMapper)
            .map(it -> it.getOrDefault(langKey, it.get(DEFAULT_LANGUAGE)));
    }

    @SneakyThrows
    private void updateTemplateSpecProps(String templateKey, String langKey, String subject, String from, String configPath) {
        CustomEmailTemplateSpec customEmailTemplateSpec = new CustomEmailTemplateSpec();
        customEmailTemplateSpec.setTemplateKey(templateKey);
        customEmailTemplateSpec.setSubjectTemplate(Map.of(langKey, subject));
        customEmailTemplateSpec.setEmailFrom(Map.of(langKey, from));
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

    @SneakyThrows
    private String mergeJsons(String targetJson, String sourceJson) {
        JsonNode targetNode = objectMapper.readValue(targetJson, JsonNode.class);
        targetNode = objectMapper.readerForUpdating(targetNode).readValue(sourceJson);

        return objectMapper.writeValueAsString(targetNode);
    }
}
