package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

public class ApiMapper {

    public static CommunicationMessageCreateWeapper from(CommunicationMessageCreate messageCreate) {
        return new CommunicationMessageCreateWeapper(messageCreate);
    }

    @RequiredArgsConstructor
    public static class CommunicationMessageCreateWeapper {
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
}
