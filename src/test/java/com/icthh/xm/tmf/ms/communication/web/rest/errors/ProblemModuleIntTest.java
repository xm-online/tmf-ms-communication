package com.icthh.xm.tmf.ms.communication.web.rest.errors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Integration test that exercises {@link ProblemModule} end-to-end: a real request hits a controller,
 * an exception is thrown / validation fails, {@link CustomExceptionTranslator} builds a Zalando
 * {@link org.zalando.problem.Problem}, and the response body is serialized by the application's
 * {@link JacksonJsonHttpMessageConverter} — which is backed by the Jackson 3 {@code JsonMapper} with the
 * {@link ProblemModule} bean registered (see {@code CommunicationJacksonConfiguration}).
 *
 * <p>Unlike {@code ProblemModuleUnitTest} (which serializes a {@link org.zalando.problem.Problem} in
 * isolation), this test asserts the actual HTTP response body produced by the full MVC pipeline,
 * proving the module turns problems into RFC 7807 documents instead of plain throwables.
 *
 * @see ProblemModule
 * @see ExceptionTranslatorIntTest
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, CommunicationApp.class})
class ProblemModuleIntTest {

    @Autowired
    private ExceptionTranslatorTestController controller;

    @Autowired
    private CustomExceptionTranslator exceptionTranslator;

    @Autowired
    private JacksonJsonHttpMessageConverter jacksonMessageConverter;

    @MockitoBean
    private SmppService smppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    @Test
    void validationFailureIsSerializedAsRfc7807Problem() throws Exception {
        mockMvc.perform(post("/test/method-argument").content("{}").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            // RFC 7807 fields written by ProblemModule.writeProblemFields
            .andExpect(jsonPath("$.type").value(ErrorConstants.CONSTRAINT_VIOLATION_TYPE.toString()))
            .andExpect(jsonPath("$.title").value("Method argument not valid"))
            .andExpect(jsonPath("$.status").value(400))
            // flattened custom parameters
            .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_VALIDATION))
            .andExpect(jsonPath("$.path").value("/test/method-argument"))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("testDTO"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("test"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"));
    }

    @Test
    void constraintViolationBodyContainsViolationsArray() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_VALIDATION))
            // produced by the ConstraintViolationProblem + Violation serializers
            .andExpect(jsonPath("$.violations[0].field").value("test"))
            .andExpect(jsonPath("$.violations[0].message").isNotEmpty());
    }

    @Test
    void parameterizedErrorFlattensCustomParametersIntoBody() throws Exception {
        mockMvc.perform(get("/test/parameterized-error2"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.message").value("test parameterized error"))
            // nested custom-parameter map is serialized via writePOJOProperty
            .andExpect(jsonPath("$.params.foo").value("foo_value"))
            .andExpect(jsonPath("$.params.bar").value("bar_value"));
    }
}
