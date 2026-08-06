package com.icthh.xm.tmf.ms.communication.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Guards the OpenAPI document springdoc serves at /v3/api-docs - the endpoint the gateway and
 * Swagger UI read. Nothing else in the suite touches it, so a broken springdoc setup used to go
 * unnoticed until runtime.
 *
 * Plain @SpringBootTest like the other IntTests here: AbstractSpringBootTest in this repo pins
 * classes = {TestConfiguration, CommunicationApp}, which lets the component scan override the
 * mocked SmppService with the real one, so its context fails to load.
 */
@SpringBootTest
public class ApiDocsIntTest {

    @Autowired
    private WebApplicationContext context;

    // the real SmppService opens an SMPP socket in its startup listener; every IntTest here mocks it
    @MockitoBean
    private SmppService smppService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void apiDocsIsServedAsOpenApi3() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
            .andExpect(jsonPath("$.paths").isNotEmpty());
    }
}
