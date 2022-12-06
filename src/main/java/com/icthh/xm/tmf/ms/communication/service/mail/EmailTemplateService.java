package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.dto.EmailTemplateDto;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
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

    private final TenantContextHolder tenantContextHolder;
    private final Configuration freeMarkerConfiguration;

    public String renderEmailContent(EmailTemplateDto emailTemplateDto) {
        try {
            String tenantKey = tenantContextHolder.getTenantKey();
            String templateKey = format("%s/en/%s", tenantKey, UUID.randomUUID());
            Template mailTemplate = new Template(templateKey, emailTemplateDto.getContent(), freeMarkerConfiguration);
            return FreeMarkerTemplateUtils.processTemplateIntoString(mailTemplate, emailTemplateDto.getModel());
        } catch (TemplateException | IOException e) {
            log.error("Template could not be rendered with content: {} and model: {}. Error: {}", emailTemplateDto.getContent(),
                emailTemplateDto.getModel(), e.getMessage());
            return null;
        }
    }
}
