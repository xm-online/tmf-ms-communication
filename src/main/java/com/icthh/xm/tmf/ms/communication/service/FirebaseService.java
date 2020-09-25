package com.icthh.xm.tmf.ms.communication.service;

import static java.util.stream.Collectors.toList;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
    public static final String BADGE_PARAMETER_NAME = "badge";

    @SneakyThrows
    public CommunicationMessage sendPushNotification(CommunicationMessage message) {
        Assert.notNull(message, "Message is not specified");

        MulticastMessage firebaseMessage = mapToFirebaseRequest(message);

        log.debug("Sending messages");

        BatchResponse response = FirebaseMessaging.getInstance()
            .sendMulticast(firebaseMessage);

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

        return buildCommunicationResponse(response);
    }

    private CommunicationMessage buildCommunicationResponse(BatchResponse batchResponse) {

        List<SendResponse> responses = batchResponse.getResponses();
        List<CommunicationRequestCharacteristic> characteristics = new ArrayList<>(responses.size());
        characteristics.add(new CommunicationRequestCharacteristic().name("successCount").value(String.valueOf(batchResponse.getSuccessCount())));
        characteristics.add(new CommunicationRequestCharacteristic().name("failureCount").value(String.valueOf(batchResponse.getFailureCount())));

        for (SendResponse response : responses) {
            String rName, rValue;
            if (response.isSuccessful()) {
                rName = "messageId";
                rValue = response.getMessageId();
            } else {
                rName = "error";
                rValue = response.getException().getMessage();
            }
            characteristics.add(new CommunicationRequestCharacteristic().name(rName).value(rValue));
        }

        return new CommunicationMessage()
            .id(UUID.randomUUID().toString())
            .characteristic(characteristics);
    }


    @SneakyThrows
    private MulticastMessage mapToFirebaseRequest(CommunicationMessage message) {
        List<String> userRegistrationTokens = message.getReceiver().stream()
            .map(Receiver::getAppUserId)
            .filter(StringUtils::isNoneBlank)
            .collect(toList());

        if (userRegistrationTokens.size() > 500) {
            throw new BusinessException("error.fcm.receiver.count", "The number of receivers exceeds 500 allowed");
        }

        Map<String, String> data = message.getCharacteristic().stream()
            .collect(Collectors.toMap(CommunicationRequestCharacteristic::getName,
                CommunicationRequestCharacteristic::getValue));

        MulticastMessage.Builder firebaseMessageBuilder = MulticastMessage.builder()
            .addAllTokens(userRegistrationTokens)
            .setNotification(Notification.builder()
                .setBody(message.getContent())
                .setTitle(message.getSubject())
                .setImage(data.get("image"))
                .build())
            .putAllData(data);

        String badge = data.get(BADGE_PARAMETER_NAME);
        if (badge != null) {
            int badgeIntValue = Integer.parseInt(badge);

            log.debug("Badge parameter is provided, adding to the request {}", badgeIntValue);

            firebaseMessageBuilder.setApnsConfig
                (ApnsConfig.builder()
                    .setAps(Aps.builder()
                        .setBadge(badgeIntValue)
                        .build())
                    .build())
                .setWebpushConfig(WebpushConfig.builder()
                    .setNotification(WebpushNotification.builder()
                        .setBadge(badge)
                        .build())
                    .build())
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setNotification(
                            AndroidNotification.builder()
                                .setNotificationCount(badgeIntValue)
                                .build()
                        )
                        .build()
                );
        }


        return firebaseMessageBuilder.build();
    }
}
