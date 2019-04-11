package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

public class ApiMapper {

    public static CommunicationMessageWeapper from(CommunicationMessage message) {
        return new CommunicationMessageWeapper(message);
    }

    @RequiredArgsConstructor
    public static class CommunicationMessageWeapper {
        private final CommunicationMessage message;
        public List<String> getPhoneNumbers() {
            return message.getReceiver().stream()
                          .map(Receiver::getPhoneNumber)
                          .collect(Collectors.toList());
        }
        public String getSenderId() {
            return message.getSender().getId();
        }
    }
}
