package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.tmf.ms.communication.web.rest.errors.ProblemModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class CommunicationJacksonConfiguration {

    @Bean
    public JacksonJsonHttpMessageConverter converter(JsonMapper jsonMapper) {
        return new JacksonJsonHttpMessageConverter(jsonMapper);
    }

    @Bean
    public ProblemModule problemModule() {
        return new ProblemModule();
    }
}
