package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.RenderTemplateException;
import freemarker.core.Environment;
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

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final Configuration freeMarkerConfiguration;

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
}
