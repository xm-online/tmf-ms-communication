package com.icthh.xm.tmf.ms.communication.web.rest;


import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateDetails;
import com.icthh.xm.tmf.ms.communication.domain.dto.TemplateMultiLangDetails;
import com.icthh.xm.tmf.ms.communication.domain.dto.UpdateTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.mail.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
@ConditionalOnProperty(name = "application.email-template-api-enabled", havingValue = "true", matchIfMissing = true)
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final EmailSpecService emailSpecService;

    @PostMapping("/render")
    @IgnoreLogginAspect
    @PreAuthorize("hasPermission({'renderTemplateRequest': #renderTemplateRequest}, 'EMAIL.TEMPLATE.RENDER')")
    @PrivilegeDescription("Privilege to render email template content")
    public RenderTemplateResponse renderEmailContentToHtml(@Valid @RequestBody RenderTemplateRequest renderTemplateRequest) {
        return emailTemplateService.renderEmailContent(renderTemplateRequest);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'EMAIL.TEMPLATE.GET_LIST')")
    @PrivilegeDescription("Privilege to get the list of email template specifications")
    public List<EmailTemplateSpec> getEmailSpec() {
        return emailSpecService.getEmailSpec().getEmails();
    }

    @GetMapping("/{templateKey}/{langKey}")
    @PreAuthorize("hasPermission({'templateKey': #templateKey, 'langKey': #langKey}, 'EMAIL.TEMPLATE.GET')")
    @PrivilegeDescription("Privilege to get email template details by key and language")
    public TemplateDetails getTemplateByKey(@PathVariable String templateKey, @PathVariable String langKey) {
        return emailTemplateService.getTemplateDetailsByKey(templateKey, langKey);
    }

    @GetMapping("/{templateKey}")
    @PreAuthorize("hasPermission({'templateKey': #templateKey}, 'EMAIL.TEMPLATE.GET')")
    @PrivilegeDescription("Privilege to get multi-language email template details by key")
    public TemplateMultiLangDetails getTemplateByKey(@PathVariable String templateKey) {
        return emailTemplateService.getTemplateMultiLangDetailsByKey(templateKey);
    }

    @PutMapping("/{templateKey}/{langKey}")
    @PreAuthorize("hasPermission({'updateTemplateRequest': #updateTemplateRequest}, 'EMAIL.TEMPLATE.UPDATE')")
    @PrivilegeDescription("Privilege to update email template")
    public void updateTemplate(@Valid @RequestBody UpdateTemplateRequest updateTemplateRequest, @PathVariable String templateKey, @PathVariable String langKey) {
        emailTemplateService.updateTemplate(templateKey, langKey, updateTemplateRequest);
    }
}
