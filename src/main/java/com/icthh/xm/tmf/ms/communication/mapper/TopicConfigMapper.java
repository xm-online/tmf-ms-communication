package com.icthh.xm.tmf.ms.communication.mapper;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.tmf.ms.communication.domain.spec.TopicKafkaQueueParamsSpec;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TopicConfigMapper {

    @Mapping(target = "typeKey", ignore = true)
    @Mapping(target = "topicName", ignore = true)
    @Mapping(target = "metadataMaxAge", ignore = true)
    @Mapping(target = "key", ignore = true)
    @Mapping(target = "consumeMessagePerSecondLimit", ignore = true)
    @Mapping(target = "autoOffsetReset", ignore = true)
    TopicConfig topicKafkaQueueParamsToConfig(TopicKafkaQueueParamsSpec topicKafkaQueueParams);
}
