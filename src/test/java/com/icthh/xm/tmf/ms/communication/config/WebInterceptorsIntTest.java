package com.icthh.xm.tmf.ms.communication.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import com.icthh.xm.commons.web.spring.TenantInterceptor;
import com.icthh.xm.commons.web.spring.XmLoggingInterceptor;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * xm-commons {@code WebMvcConfig} registers the tenant, logging and LEP interceptors. Registering them
 * a second time made {@code TenantInterceptor.afterCompletion} run twice per request: the second run found
 * the tenant context already destroyed and logged "Tenant context doesn't have tenant key" as ERROR.
 */
@SpringBootTest
public class WebInterceptorsIntTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    // the real SmppService opens an SMPP socket in its startup listener; every IntTest here mocks it
    @MockitoBean
    private SmppService smppService;

    @Test
    public void apiRequestHasEachXmInterceptorOnce() throws Exception {
        List<HandlerInterceptor> interceptors = interceptorsFor("POST", "/api/templates/render");

        assertThat(count(interceptors, TenantInterceptor.class)).isEqualTo(1);
        assertThat(count(interceptors, XmLoggingInterceptor.class)).isEqualTo(1);
        assertThat(count(interceptors, LepInterceptor.class)).isEqualTo(1);
    }

    @Test
    public void tenantIgnoredPathHasNoTenantOrLepInterceptor() throws Exception {
        List<HandlerInterceptor> interceptors = interceptorsFor("GET", "/v3/api-docs");

        assertThat(count(interceptors, TenantInterceptor.class)).isZero();
        assertThat(count(interceptors, LepInterceptor.class)).isZero();
    }

    private List<HandlerInterceptor> interceptorsFor(String method, String path) throws Exception {
        HandlerExecutionChain chain = handlerMapping.getHandler(new MockHttpServletRequest(method, path));
        assertThat(chain).as("handler for " + path).isNotNull();
        return chain.getInterceptorList();
    }

    private static long count(List<HandlerInterceptor> interceptors, Class<?> type) {
        return interceptors.stream().filter(type::isInstance).count();
    }
}
