package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import com.icthh.xm.commons.web.spring.config.XmWebMvcConfigurerAdapter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
public class WebMvcConfiguration extends XmWebMvcConfigurerAdapter {

    private final LepInterceptor lepInterceptor;
    private final ApplicationProperties appProps;

    protected WebMvcConfiguration(LepInterceptor lepInterceptor,
                                  TenantInterceptor tenantInterceptor,
                                  XmLoggingInterceptor xmLoggingInterceptor,
                                  ApplicationProperties appProps) {
        super(tenantInterceptor, xmLoggingInterceptor);
        this.lepInterceptor = lepInterceptor;
        this.appProps = appProps;
    }

    @Override
    protected void xmAddInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(lepInterceptor).addPathPatterns("/**");
    }

    @Override
    protected void xmConfigurePathMatch(PathMatchConfigurer configurer) {

    }

    @Override
    protected List<String> getTenantIgnorePathPatterns() {
        return appProps.getTenantIgnoredPathList();
    }
}
