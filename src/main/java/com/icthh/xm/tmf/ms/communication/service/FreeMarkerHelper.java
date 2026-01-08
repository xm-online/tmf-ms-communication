package com.icthh.xm.tmf.ms.communication.service;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import freemarker.template.Template;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import java.io.StringReader;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FreeMarkerHelper {

    private final Configuration freeMarkerConfiguration;

    public String processTemplate(String templateName, String value, Map<String, Object> params) throws IOException, TemplateException {
        Template template = new Template(templateName, new StringReader(value), freeMarkerConfiguration);
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
    }

}
