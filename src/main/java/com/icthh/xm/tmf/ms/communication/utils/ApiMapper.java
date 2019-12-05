package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApiMapper {

    public static CommunicationMessageWrapper from(CommunicationMessage message) {
        MessageType messageType = MessageType.valueOf(message.getType());
        return new CommunicationMessageWrapper(messageType, message.getReceiver(), message.getSubject(), message.getDescription(), message.getCharacteristic());
    }

    public static CommunicationMessageWrapper from(CommunicationMessageCreate message) {
        MessageType messageType = MessageType.valueOf(message.getType());
        return new CommunicationMessageWrapper(messageType, message.getReceiver(), message.getSubject(), message.getDescription(), message.getCharacteristic());
    }

    @RequiredArgsConstructor
    public static class CommunicationMessageWrapper {

        public static final String DELIVERY_REPORT = "DELIVERY.REPORT";

        @Getter
        private final MessageType type;

        @Getter
        private final List<Receiver> receivers;
        private final String title;
        private final String content;

        @Getter
        private final List<CommunicationRequestCharacteristic> characteristics;

        public List<String> getPhoneNumbers() {
            return receivers.stream()
                .map(Receiver::getPhoneNumber)
                .collect(Collectors.toList());
        }

        public byte getDeliveryReport() {
            return Optional.ofNullable(characteristics).orElse(Collections.emptyList())
                .stream()
                .filter(c -> DELIVERY_REPORT.equals(c.getName()))
                .findFirst()
                .map(c -> NumberUtils.toByte(c.getValue(), (byte) 0))
                .orElse((byte) 0);
        }
    }
}
