package com.icthh.xm.tmf.ms.communication.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

@Component
public class DtoMapperConfiguration {

    public DtoMapperConfiguration(ObjectMapper objectMapper) {
        registerSubclasses(objectMapper);
    }

    @SneakyThrows
    private void registerSubclasses(ObjectMapper objectMapper) {
        var provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(KeyboardDto.class));
        var components = provider.findCandidateComponents("com.icthh.xm.tmf.ms.communication.dto");
        for (BeanDefinition component : components) {
            Class<?> cls = Class.forName(component.getBeanClassName());
            objectMapper.registerSubtypes(cls);
        }
    }

}
