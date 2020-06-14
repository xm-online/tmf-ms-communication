package com.icthh.xm.tmf.ms.communication.config.kafka;

import org.testcontainers.containers.KafkaContainer;

public class KafkaContainerStaticKeeper {
    private static KafkaContainer kafkaContainer;

    public static KafkaContainer keepContainer(KafkaContainer kafkaContainer) {
        return KafkaContainerStaticKeeper.kafkaContainer = kafkaContainer;
    }

    public static KafkaContainer getKafkaContainer() {
        return kafkaContainer;
    }
}
