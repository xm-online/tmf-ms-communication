package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.tmf.ms.communication.web.api.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;

public class ApiMapper {

    public static CommunicationMessageWrapper from(CommunicationMessage message) {
        return new CommunicationMessageWrapper(message.getReceiver(), message.getCharacteristic());
    }

    public static CommunicationMessageWrapper from(CommunicationMessageCreate message) {
        return new CommunicationMessageWrapper(message.getReceiver(), message.getCharacteristic());
    }

    @RequiredArgsConstructor
    public static class CommunicationMessageWrapper {

        public static final String DELIVERY_REPORT = "DELIVERY.REPORT";

        private final List<Receiver> receivers;
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
