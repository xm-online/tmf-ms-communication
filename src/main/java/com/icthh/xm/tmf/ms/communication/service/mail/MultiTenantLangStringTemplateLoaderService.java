package com.icthh.xm.tmf.ms.communication.service.mail;

import freemarker.cache.StringTemplateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MultiTenantLangStringTemplateLoaderService {

    private final Map<String, Map<String, StringTemplateLoader>> loadersByTenant = new ConcurrentHashMap<>();

    public void putTemplate(String name, String content, String tenantKey, String lang) {
        getTemplateLoader(tenantKey, lang).putTemplate(name, content);
    }

    public void removeTemplate(String name, String tenantKey, String lang) {
        getTemplateLoader(tenantKey, lang).removeTemplate(name);
    }

    public StringTemplateLoader getTemplateLoader(String tenantKey, String lang) {
        return loadersByTenant.computeIfAbsent(tenantKey, key -> new ConcurrentHashMap<>())
            .computeIfAbsent(lang, key -> new StringTemplateLoader());
    }

}
