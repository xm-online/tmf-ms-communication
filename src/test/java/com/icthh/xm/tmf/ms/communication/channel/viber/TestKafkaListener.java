package com.icthh.xm.tmf.ms.communication.channel.viber;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.LinkedList;
import java.util.List;

@TestComponent
@Slf4j
public class TestKafkaListener {

    private List<Listener> listeners = new LinkedList<>();

    @KafkaListener(id = "test", topics = {
        "#{'${application.messaging.sent-queue-name}'}",
        "#{'${application.messaging.send-failed-queue-name}'}",
        "#{'${application.messaging.delivered-queue-name}'}",
        "#{'${application.messaging.delivery-failed-queue-name}'}"
    })
    public void listen(@Payload String message,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.debug("Consumed message. value {}", message);

        for (TestKafkaListener.Listener listener : listeners) {
            listener.accept(topic, message);
        }
    }

    public void addListener(TestKafkaListener.Listener listener) {
        listeners.add(listener);
    }

    public interface Listener {
        void accept(String topic, String value);
    }

}
