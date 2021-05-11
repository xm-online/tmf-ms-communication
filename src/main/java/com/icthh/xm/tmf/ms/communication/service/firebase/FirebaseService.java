package com.icthh.xm.tmf.ms.communication.service.firebase;

import static java.util.stream.Collectors.toList;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.channel.mobileapp.FirebaseApplicationConfigurationProvider;
import com.icthh.xm.tmf.ms.communication.service.MobileAppMessagePayloadCustomizationService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseApplicationConfigurationProvider.class)
public class FirebaseService {

    private final FirebaseApplicationConfigurationProvider firebaseApplicationConfigurationProvider;
    private final TenantContextHolder tenantContextHolder;
    private final MobileAppMessagePayloadCustomizationService payloadCustomizer;
    private final List<MessageConfigurator> messageConfigurators;

    @SneakyThrows
    public CommunicationMessage sendPushNotification(CommunicationMessage message) {
        Assert.notNull(message, "Message is not specified");

        MulticastMessage firebaseMessage = mapToFirebaseRequest(message);

        log.debug("Sending messages");

        BatchResponse response = getFirebaseMessaging(message)
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

        return buildCommunicationResponse(response, message);
    }

    @SneakyThrows
    private MulticastMessage mapToFirebaseRequest(CommunicationMessage message) {
        if (CollectionUtils.isEmpty(message.getReceiver())) {
            throw new BusinessException("error.fcm.receiver.empty", "Receiver list is empty");
        }

        List<String> userRegistrationTokens = getTokens(message);

        Map<String, String> rawData = Optional.ofNullable(message.getCharacteristic())
            .orElse(Collections.emptyList()).stream()
            .collect(Collectors.toMap(CommunicationRequestCharacteristic::getName,
                CommunicationRequestCharacteristic::getValue));

        BuilderWrapper builder = new BuilderWrapper();

        builder.getNotificationBuilder()
            .setBody(message.getContent())
            .setTitle(message.getSubject());

        messageConfigurators.forEach(cr -> cr.apply(builder, message, rawData));

        return builder.getFirebaseMessageBuilder()
            .setApnsConfig(builder.getApnsBuilder()
                .setAps(builder.getApsBuilder().build()).build())
            .setWebpushConfig(builder.getWebPushBuilder()
                .setNotification(builder.getWebpushNotificationBuilder().build())
                .build())
            .setAndroidConfig(builder.getAndroidConfigBuilder()
                .setNotification(builder.getAndroidNotificationBuilder().build())
                .build())
            .setNotification(builder.getNotificationBuilder().build())
            .addAllTokens(userRegistrationTokens)
            .putAllData(payloadCustomizer.customizePayload(rawData))
            .build();
    }

    private List<String> getTokens(CommunicationMessage message) {
        List<String> userRegistrationTokens = message.getReceiver().stream()
            .map(Receiver::getAppUserId)
            .filter(StringUtils::isNoneBlank)
            .collect(toList());

        if (userRegistrationTokens.isEmpty()) {
            throw new BusinessException("error.fcm.receiver.invalid", "Receiver list - no appUserId specified");
        } else if (userRegistrationTokens.size() > 500) {
            throw new BusinessException("error.fcm.receiver.count", "The number of receivers exceeds 500 allowed");
        }
        return userRegistrationTokens;
    }

    private FirebaseMessaging getFirebaseMessaging(CommunicationMessage message) {
        return firebaseApplicationConfigurationProvider.getFirebaseMessaging(
            tenantContextHolder.getTenantKey(), message.getSender().getId())
            .orElseThrow(() -> new BusinessException("error.fcm.sender.id.invalid", "Sender id is not valid"));
    }

    private CommunicationMessage buildCommunicationResponse(BatchResponse batchResponse, CommunicationMessage message) {
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

        characteristics.addAll(message.getCharacteristic());

        return message
            .id(UUID.randomUUID().toString())
            .characteristic(characteristics);
    }

}
