package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.tmf.ms.communication.domain.dto.EmailTemplateDto;
import com.icthh.xm.tmf.ms.communication.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/templates")
public class TenantEmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @PostMapping("/render")
    public ResponseEntity<String> renderEmailContentToHtml(@Valid @RequestBody EmailTemplateDto emailTemplateDto) {
        String htmlContent = emailTemplateService.renderEmailContent(emailTemplateDto);
        return ResponseEntity.ok(htmlContent);
    }

}
