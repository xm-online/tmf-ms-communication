package com.icthh.xm.tmf.ms.communication.mapper;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.tmf.ms.communication.domain.spec.TopicKafkaQueueParamsSpec;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TopicConfigMapper {
    TopicConfig topicKafkaQueueParamsToConfig(TopicKafkaQueueParamsSpec topicKafkaQueueParams);
}
