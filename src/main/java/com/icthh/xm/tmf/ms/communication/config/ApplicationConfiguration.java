package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfiguration;
import com.icthh.xm.commons.topic.service.DynamicConsumerConfigurationService;
import com.icthh.xm.commons.topic.service.TopicManagerService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DynamicConsumerConfigurationService dynamicConsumerConfigurationService(List<DynamicConsumerConfiguration> dynamicConsumerConfigurations,
                                                                                   TopicManagerService topicManagerService,
                                                                                   TenantListRepository tenantListRepository) {
        return new DynamicConsumerConfigurationService(dynamicConsumerConfigurations, topicManagerService, tenantListRepository);
    }
}
