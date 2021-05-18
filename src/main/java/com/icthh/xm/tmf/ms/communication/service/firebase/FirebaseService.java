package com.icthh.xm.tmf.ms.communication.service.firebase;

import static java.util.stream.Collectors.toList;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.channel.mobileapp.FirebaseApplicationConfigurationProvider;
import com.icthh.xm.tmf.ms.communication.service.MobileAppMessagePayloadCustomizationService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Detail;
import com.icthh.xm.tmf.ms.communication.web.api.model.ErrorDetail;
import com.icthh.xm.tmf.ms.communication.web.api.model.ExtendedCommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Result;
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

        List<Receiver> receivers = getReceiversAndValidate(message);
        MulticastMessage firebaseMessage = mapToFirebaseRequest(message, receivers);

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

        return buildCommunicationResponse(response, message, receivers);
    }

    @SneakyThrows
    private MulticastMessage mapToFirebaseRequest(CommunicationMessage message, List<Receiver> receivers) {
        Map<String, String> rawData = Optional.ofNullable(message.getCharacteristic())
            .orElseGet(Collections::emptyList).stream()
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
            .addAllTokens(receivers.stream()
                .map(Receiver::getAppUserId)
                .collect(toList()))
            .putAllData(payloadCustomizer.customizePayload(rawData))
            .build();
    }

    private List<Receiver> getReceiversAndValidate(CommunicationMessage message) {
        if (CollectionUtils.isEmpty(message.getReceiver())) {
            throw new BusinessException(ErrorCodes.RECEIVER_EMPTY, "Receiver list is empty");
        }

        List<Receiver> receivers = message.getReceiver().stream()
            .filter(Objects::nonNull)
            .filter(r -> StringUtils.isNoneBlank(r.getAppUserId()))
            .collect(toList());

        if (receivers.isEmpty()) {
            throw new BusinessException(ErrorCodes.RECEIVER_INVALID, "Receiver list - no appUserId specified");
        } else if (receivers.size() > 500) {
            throw new BusinessException(ErrorCodes.RECEIVER_COUNT, "The number of receivers exceeds 500 allowed");
        }
        return receivers;
    }

    private FirebaseMessaging getFirebaseMessaging(CommunicationMessage message) {
        return firebaseApplicationConfigurationProvider.getFirebaseMessaging(
            tenantContextHolder.getTenantKey(), message.getSender().getId())
            .orElseThrow(() -> new BusinessException(ErrorCodes.SENDER_ID_INVALID, "Sender id is not valid"));
    }

    private CommunicationMessage buildCommunicationResponse(BatchResponse batchResponse, CommunicationMessage msg,
                                                            List<Receiver> receivers) {
        ExtendedCommunicationMessage message = ExtendedCommunicationMessageFactory.newMessage(msg);

        List<Detail> details = new ArrayList<>();
        List<SendResponse> responses = batchResponse.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);

            Detail detail = new Detail()
                .receiver(receivers.get(i));

            if (response.isSuccessful()) {
                detail.status(Detail.Status.SUCCESS)
                    .messageId(response.getMessageId());
            } else {
                FirebaseMessagingException exception = response.getException();

                detail.status(Detail.Status.ERROR)
                    .error(new ErrorDetail()
                        .code(String.valueOf(exception.getMessagingErrorCode()))
                        .description(exception.getMessage()));
            }
            details.add(detail);
        }

        message.result(new Result()
            .successCount(batchResponse.getSuccessCount())
            .failureCount(batchResponse.getFailureCount())
            .details(details));

        return message.id(UUID.randomUUID().toString());
    }

    static final class ErrorCodes {
        public static final String RECEIVER_EMPTY = "error.fcm.receiver.empty";
        public static final String RECEIVER_INVALID = "error.fcm.receiver.invalid";
        public static final String RECEIVER_COUNT = "error.fcm.receiver.count";
        public static final String SENDER_ID_INVALID = "error.fcm.sender.id.invalid";
    }

}
