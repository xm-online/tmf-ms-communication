package com.icthh.xm.tmf.ms.communication.service;

import static java.util.stream.Collectors.toList;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseService {

    @SneakyThrows
    public void sendPushNotification(CommunicationMessage message) { //todo V: return type?
//todo V: 500 receivers cap
        Assert.notNull(message, "Message is not specified");

        List<String> userRegistrationTokens = message.getReceiver().stream()
            .map(Receiver::getAppUserId)
            .filter(StringUtils::isNoneBlank)
            .collect(toList());

        Map<String, String> data = message.getCharacteristic().stream()
            .collect(Collectors.toMap(CommunicationRequestCharacteristic::getName,
                CommunicationRequestCharacteristic::getValue));

        MulticastMessage firebaseMessage = MulticastMessage.builder()
            .putAllData(data)
            .setNotification(Notification.builder()
                .setBody(message.getContent())
                .setTitle(message.getSubject())
                .build())
            .addAllTokens(userRegistrationTokens)
            .build();

        BatchResponse response = FirebaseMessaging.getInstance()
            .sendMulticast(firebaseMessage); //todo V: think over exception handling

        log.debug("Total messages {}, success count {}, failure count {}",
            response.getResponses().size(), response.getSuccessCount(), response.getFailureCount());

        if (response.getFailureCount() != 0) {
            log.info("Error response details {}", response.getResponses().stream()
                .map(SendResponse::getException)
                .filter(Objects::nonNull)
                .map(e -> String.format("%s: %s",
                    e.getMessagingErrorCode(), e.getMessage()))
                .collect(Collectors.toList())
            );
        }
    }
}
