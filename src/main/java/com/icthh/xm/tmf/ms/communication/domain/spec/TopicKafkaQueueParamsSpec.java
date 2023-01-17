package com.icthh.xm.tmf.ms.communication.domain.spec;

import lombok.Data;

@Data
public class TopicKafkaQueueParamsSpec {
    private Integer retriesCount;
    private Long backOffPeriod;
    private String deadLetterQueue;
    private Boolean logBody = true;
    private Integer maxPollInterval;
    private String isolationLevel;
}
