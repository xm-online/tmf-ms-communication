package com.icthh.xm.tmf.ms.communication.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.tmf.ms.communication.domain.dto.EmailTemplateDto;
import com.icthh.xm.tmf.ms.communication.service.mail.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @Timed
    @PostMapping("/render")
    public ResponseEntity<String> renderEmailContentToHtml(@Valid @RequestBody EmailTemplateDto emailTemplateDto) {
        String htmlContent = emailTemplateService.renderEmailContent(emailTemplateDto);
        return ResponseEntity.ok(htmlContent);
    }

}
