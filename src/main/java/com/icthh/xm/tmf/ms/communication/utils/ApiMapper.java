package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

public class ApiMapper {

    public static CommunicationMessageCreateWrapper from(CommunicationMessageCreate message) {
        return new CommunicationMessageCreateWrapper(message);
    }

    @RequiredArgsConstructor
    public static class CommunicationMessageCreateWrapper {
        private final CommunicationMessageCreate message;
        public List<String> getPhoneNumbers() {
            return message.getReceiver().stream()
                          .map(Receiver::getPhoneNumber)
                          .collect(Collectors.toList());
        }
        public String getSenderId() {
            return message.getSender().getId();
        }
    }

    public static CommunicationMessageWrapper from(CommunicationMessage message) {
        return new CommunicationMessageWrapper(message);
    }

    @RequiredArgsConstructor
    public static class CommunicationMessageWrapper {
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
