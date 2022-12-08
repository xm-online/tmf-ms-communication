package com.icthh.xm.tmf.ms.communication.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateRequest;
import com.icthh.xm.tmf.ms.communication.domain.dto.RenderTemplateResponse;
import com.icthh.xm.tmf.ms.communication.domain.spec.EmailTemplateSpec;
import com.icthh.xm.tmf.ms.communication.service.EmailSpecService;
import com.icthh.xm.tmf.ms.communication.service.mail.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final EmailSpecService emailSpecService;

    @Timed
    @PostMapping("/render")
    public RenderTemplateResponse renderEmailContentToHtml(@Valid @RequestBody RenderTemplateRequest renderTemplateRequest) {
        return emailTemplateService.renderEmailContent(renderTemplateRequest);
    }

    @Timed
    @GetMapping
    public List<EmailTemplateSpec> getEmailSpec() {
        return emailSpecService.getEmailSpec().getEmails();
    }

}
