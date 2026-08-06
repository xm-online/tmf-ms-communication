package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.tmf.ms.communication.web.rest.errors.ProblemModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunicationJacksonConfiguration {


    @Bean
    public ProblemModule problemModule() {
        return new ProblemModule();
    }
}
