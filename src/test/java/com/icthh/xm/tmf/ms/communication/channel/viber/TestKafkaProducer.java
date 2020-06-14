package com.icthh.xm.tmf.ms.communication.channel.viber;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.core.KafkaTemplate;

@TestComponent
public class TestKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public TestKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, Object message) {
        if (message instanceof String) {
            kafkaTemplate.send(topic, (String) message);
        } else {
            kafkaTemplate.send(topic, new Gson().toJson(message));
        }
    }

    public void sendMessage(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, new Gson().toJson(message));
    }
}
