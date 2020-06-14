package com.icthh.xm.tmf.ms.communication.config.kafka;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.KafkaContainer;

public class KafkaContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        KafkaContainer kafkaContainer = KafkaContainerStaticKeeper.getKafkaContainer();
        if (!kafkaContainer.isRunning()) {
            kafkaContainer.start();
        }
        Integer mappedPort = KafkaContainerStaticKeeper.getKafkaContainer().getMappedPort(9093);
        addProperty(ctx, "spring.kafka.bootstrap-servers:" + "localhost:" + mappedPort);
    }

    private void addProperty(ConfigurableApplicationContext configurableApplicationContext, String s) {
        TestPropertyValues
                .of(s)
                .applyTo(configurableApplicationContext);
    }
}
