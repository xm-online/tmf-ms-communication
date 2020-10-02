package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;

import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    public static Map<String, String> collectMessageCharacteristics(CommunicationMessage message) {
        return message.getCharacteristic()
            .stream()
            .filter(characteristic -> characteristic.getName() != null && characteristic.getValue() != null)
            .collect(Collectors.toMap(
                CommunicationRequestCharacteristic::getName,
                CommunicationRequestCharacteristic::getValue)
            );
    }
}
