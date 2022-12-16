package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final Configuration freeMarkerConfiguration;
    private final StringTemplateLoader templateLoader;
    private final MultiTenantLangStringTemplateLoaderService multiTenantLangStringTemplateLoaderService;
    private final TenantContextHolder tenantContextHolder;

    @SneakyThrows
    public RenderTemplateResponse renderEmailContent(RenderTemplateRequest renderTemplateRequest) {
        String tenantKey = tenantContextHolder.getTenantKey();
        String renderedContent = processEmailTemplate(tenantKey, renderTemplateRequest.getContent(), renderTemplateRequest.getModel(), renderTemplateRequest.getLang());
        RenderTemplateResponse renderTemplateResponse = new RenderTemplateResponse();
        renderTemplateResponse.setContent(renderedContent);
        return renderTemplateResponse;
    }

    public String processEmailTemplate(String tenantKey, String content, Map<String, Object> objectModel, String lang) {
        try {
            Configuration configuration = (Configuration) freeMarkerConfiguration.clone();
            StringTemplateLoader templateLoaderByTenantAndLang = multiTenantLangStringTemplateLoaderService.getTemplateLoader(tenantKey, lang);
            MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(
                new TemplateLoader[]{templateLoaderByTenantAndLang, templateLoader}
            );
            configuration.setTemplateLoader(multiTemplateLoader);

            Template mailTemplate = new Template(UUID.randomUUID().toString(), content, configuration);
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
}
