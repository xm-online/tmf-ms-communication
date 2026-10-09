package com.icthh.xm.tmf.ms.communication.config;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@Configuration
public class XmFreeMarkerConfiguration {

    @Bean
    public freemarker.template.Configuration freeMarkerConfiguration(XmFreeMarkerConfigurer xmFreeMarkerConfigurer) {
        freemarker.template.Configuration configuration = xmFreeMarkerConfigurer.getConfiguration();

        configuration.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        configuration.setAPIBuiltinEnabled(false);

        BeansWrapperBuilder wrapperBuilder = new BeansWrapperBuilder(freemarker.template.Configuration.VERSION_2_3_34);
        wrapperBuilder.setExposureLevel(BeansWrapper.EXPOSE_SAFE);
        configuration.setObjectWrapper(wrapperBuilder.build());

        return configuration;
    }

    @Bean
    public XmFreeMarkerConfigurer xmFreeMarkerConfigurer(StringTemplateLoader emailTemplates) {
        return new XmFreeMarkerConfigurer(emailTemplates);
    }

    @Bean
    public StringTemplateLoader emailTemplates() {
        return new StringTemplateLoader();
    }

    @RequiredArgsConstructor
    public static class XmFreeMarkerConfigurer extends FreeMarkerConfigurer {

        private final StringTemplateLoader emailTemplates;

        @Override
        protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
            super.postProcessTemplateLoaders(templateLoaders);
            templateLoaders.add(emailTemplates);
        }
    }
}
