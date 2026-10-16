package com.icthh.xm.tmf.ms.communication.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.icthh.xm.tmf.ms.communication.service.SmppService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * With springdoc.api-docs.enabled=false (the prod default) no API specification may be served,
 * whoever asks: the attacker in the incident held a super-admin token, so authorization alone
 * does not hide the API surface.
 */
@SpringBootTest(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
public class ApiDocsDisabledIntTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private SmppService smppService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void specificationEndpointsAreNotServed() throws Exception {
        List<String> paths = List.of("/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html",
            "/swagger-ui/index.html", "/v2/api-docs", "/api.yml");
        for (String path : paths) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isGreaterThanOrEqualTo(400);
        }
    }

    @Test
    public void springdocBeansAreNotRegistered() {
        assertThat(context.getBeanNamesForType(org.springdoc.webmvc.api.OpenApiWebMvcResource.class)).isEmpty();
    }
}
