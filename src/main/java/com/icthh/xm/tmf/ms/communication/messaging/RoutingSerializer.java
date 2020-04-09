package com.icthh.xm.tmf.ms.communication.messaging;

import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class RoutingSerializer implements Serializer<Object> {

    private StringSerializer stringSerializer = new StringSerializer();
    private JsonSerializer<Object> objectSerializer = new JsonSerializer<>();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        stringSerializer.configure(configs, isKey);
        objectSerializer.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data instanceof CharSequence) {
            return stringSerializer.serialize(topic, String.valueOf(data));
        }
        return objectSerializer.serialize(topic, data);
    }

    @Override
    public void close() {
        stringSerializer.close();
        objectSerializer.close();
    }
}
