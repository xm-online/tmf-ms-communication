package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.commons.permission.access.XmPermissionEvaluator;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.commons.security.spring.config.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/**
 * Resource server / method security wiring.
 *
 * <p>JWT validation, CSRF/headers/session and the default URL authorization rules
 * (which already cover communication's {@code /api/**} and {@code /management/**}
 * endpoints) are provided by the xm-commons {@link SecurityConfiguration} base class
 * via the injected {@link TokenProvider}.
 */
@Configuration
public class XmSecurityConfiguration extends SecurityConfiguration {

    public XmSecurityConfiguration(TokenProvider tokenProvider,
                                   @Value("${jhipster.security.content-security-policy}")
                                   String contentSecurityPolicy) {
        super(tokenProvider, contentSecurityPolicy);
    }

    /**
     * Enables {@code hasPermission(...)} SpEL used in {@code @PreAuthorize} across the
     * web layer by wiring the XM permission evaluator into method security.
     */
    @Bean
    @Primary
    static MethodSecurityExpressionHandler expressionHandler(XmPermissionEvaluator customPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(customPermissionEvaluator);
        return expressionHandler;
    }
}
