package com.icthh.xm.tmf.ms.communication.service.mail;

import freemarker.cache.StringTemplateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MultiLangStringTemplateLoaderService {

    private final Map<String, StringTemplateLoader> loadersByLang = new ConcurrentHashMap<>();

    public void putTemplate(String name, String content, String lang) {
        getTemplateLoader(lang).putTemplate(name, content);
    }

    public void removeTemplate(String name, String lang) {
        getTemplateLoader(lang).removeTemplate(name);
    }

    public StringTemplateLoader getTemplateLoader(String lang) {
        return loadersByLang.computeIfAbsent(lang, (key) -> new StringTemplateLoader());
    }

}
