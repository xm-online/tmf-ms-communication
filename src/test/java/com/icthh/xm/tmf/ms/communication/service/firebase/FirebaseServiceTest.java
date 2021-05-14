package com.icthh.xm.tmf.ms.communication.service.firebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
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
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.util.ReflectionUtils;


public class FirebaseServiceTest {
    public static final String SENDER_ID = "sender-id";
    public static final String TENANT_KEY = "tenant-key";
    public static final String FCM_MESSAGE_ID = "fcm-message-id";
    public static final String TEST_CONTENT = "test-content";
    public static final String TEST_SUBJECT = "test-subject";
    public static final String RECEIVER_ID_1 = "receiver-id-1";
    public static final String RECEIVER_ID_2 = "receiver-id-2";
    public static final String APP_USER_ID_1 = "app-user-id-1";
    public static final String APP_USER_ID_2 = "app-user-id-2";
    public static final String CUSTOM_NAME = "custom-name";
    public static final String CUSTOM_VALUE = "custom-value";
    public static final String CUSTOMIZED_KEY = "customized-key";
    public static final String CUSTOMIZED_VALUE = "customized-value";
    public static final String VALIDITY_PERIOD_NAME = "VALIDITY.PERIOD";
    public static final String VALIDITY_SECONDS = "300";
    public static final String BADGE_NAME = "BADGE";
    public static final String BADGE_VALUE = "1";

    private TenantContextHolder tenantContextHolder = mock(TenantContextHolder.class);
    private FirebaseApplicationConfigurationProvider configurationProvider =
        mock(FirebaseApplicationConfigurationProvider.class);
    private MobileAppMessagePayloadCustomizationService payloadCustomizationServiceMock =
        mock(MobileAppMessagePayloadCustomizationService.class);

    private FirebaseService firebaseService = new FirebaseService(configurationProvider,
        tenantContextHolder, payloadCustomizationServiceMock, List.of(
        new ValidityPeriodConfigurator(), new BadgeConfigurator(), new ImageConfigurator()
    ));

    @Test
    @SneakyThrows
    public void happyPath() {
        //given:
        FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);

        when(configurationProvider.getFirebaseMessaging(TENANT_KEY, SENDER_ID))
            .thenReturn(Optional.of(messagingMock));

        when(tenantContextHolder.getTenantKey())
            .thenReturn(TENANT_KEY);

        when(messagingMock.sendMulticast(any(MulticastMessage.class)))
            .thenReturn(TestBatchResponse.builder()
                .responses(List.of(
                    newSendResponse(FCM_MESSAGE_ID),
                    newFailedSendResponse()))
                .successCount(1)
                .failureCount(0)
                .build());

        doAnswer(invocation -> {
            HashMap<Object, Object> answer = new HashMap<>(invocation.getArgument(0));
            answer.put(CUSTOMIZED_KEY, CUSTOMIZED_VALUE);
            return answer;
        }).when(payloadCustomizationServiceMock).customizePayload(anyMap());

        CommunicationMessage message = new CommunicationMessage()
            .sender(new Sender()
                .id(SENDER_ID))
            .type("type")
            .content(TEST_CONTENT)
            .subject(TEST_SUBJECT)
            .receiver(List.of(
                new Receiver()
                    .id(RECEIVER_ID_1)
                    .appUserId(APP_USER_ID_1),
                new Receiver()
                    .id(RECEIVER_ID_2)
                    .appUserId(APP_USER_ID_2)
                )
            )
            .characteristic(List.of(
                new CommunicationRequestCharacteristic()
                    .name(CUSTOM_NAME)
                    .value(CUSTOM_VALUE),
                new CommunicationRequestCharacteristic()
                    .name(VALIDITY_PERIOD_NAME)
                    .value(VALIDITY_SECONDS),
                new CommunicationRequestCharacteristic()
                    .name(BADGE_NAME)
                    .value(BADGE_VALUE)
            ));

        Instant beforeTest = Instant.now();

        //when:
        ExtendedCommunicationMessage messageResult = (ExtendedCommunicationMessage) firebaseService.sendPushNotification(message);

        //then:
        assertNotNull(messageResult);

        //verify FCM message:
        ArgumentCaptor<MulticastMessage> multicastCaptor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(messagingMock).sendMulticast(multicastCaptor.capture());
        verifyNoMoreInteractions(messagingMock);

        MulticastMessage multicastMessage = multicastCaptor.getValue();

        assertEquals(extractField("tokens", multicastMessage), List.of(APP_USER_ID_1, APP_USER_ID_2));
        assertEquals(Map.of(CUSTOMIZED_KEY, CUSTOMIZED_VALUE,
            CUSTOM_NAME, CUSTOM_VALUE,
            VALIDITY_PERIOD_NAME, VALIDITY_SECONDS,
            BADGE_NAME, BADGE_VALUE),
            extractField("data", multicastMessage));

        Notification notifications = (Notification) extractField("notification", multicastMessage);
        assertEquals(TEST_SUBJECT, extractField("title", notifications));
        assertEquals(TEST_CONTENT, extractField("body", notifications));

        Object androidConfig = extractField("androidConfig", multicastMessage);
        Object apnsConfig = extractField("apnsConfig", multicastMessage);

        //check validity period
        assertEquals(VALIDITY_SECONDS + "s", extractField("ttl", androidConfig));

        Map<String, String> apnHeaders = (Map<String, String>) extractField("headers", apnsConfig);
        Object expiration = apnHeaders.get("apns-expiration");
        assertNotNull(expiration);
        assertEquals(0, Duration.between(Instant.ofEpochMilli(Long.parseLong((String) expiration) * 1000),
            beforeTest.plus(300, ChronoUnit.SECONDS)).toSeconds());

        //check badge
        assertEquals(Integer.valueOf(BADGE_VALUE),
            Optional.ofNullable(((Map<String, Object>) extractField("payload", apnsConfig)).get("aps"))
                .map(e -> ((Map<String, Object>) e).get("badge"))
                .orElseThrow());


        //verify the response:
        Result result = messageResult.result();
        assertNotNull(result);
        Assertions.assertEquals(1, (int) result.successCount());
        assertEquals(0, (int) result.failureCount());
        List<Detail> details = result.details();
        assertEquals(details, List.of(
            new Detail()
                .status(Detail.Status.SUCCESS)
                .messageId(FCM_MESSAGE_ID)
                .receiver(new Receiver().id(RECEIVER_ID_1).appUserId(APP_USER_ID_1)),
            new Detail()
                .status(Detail.Status.ERROR)
                .error(new ErrorDetail()
                    .code("UNREGISTERED")
                    .description("msg"))
                .receiver(new Receiver().id(RECEIVER_ID_2).appUserId(APP_USER_ID_2))

        ));
    }

    @Test
    void shouldThrowAnExceptionIfSendIdIsNotValid() {
        //given:
        when(configurationProvider.getFirebaseMessaging(anyString(), anyString()))
            .thenReturn(null);

        try {
            //when:
            firebaseService.sendPushNotification(new CommunicationMessage()
                .sender(new Sender()
                    .id("sender-id"))
                .receiver(List.of(
                    new Receiver()
                        .id("receiver-id")
                        .appUserId("app-user-id")
                    )
                )
                .characteristic(null));
            fail();
        } catch (BusinessException e) {
            //then:
            assertEquals("error.fcm.sender.id.invalid", e.getCode());
            assertEquals("Sender id is not valid", e.getMessage());
        }
    }

    @Test
    void shouldThrowAnExceptionIfNoReceiversProvided() {
        try {
            firebaseService.sendPushNotification(new CommunicationMessage().receiver(null));
            fail();
        } catch (BusinessException e) {
            assertEquals("error.fcm.receiver.empty", e.getCode());
            assertEquals("Receiver list is empty", e.getMessage());
        }
    }

    @Test
    void shouldThrowAnExceptionIfNoReceiverAppIdProvided() {
        try {
            firebaseService.sendPushNotification(new CommunicationMessage()
                .receiver(List.of(new Receiver().id("id"))));
            fail();
        } catch (BusinessException e) {
            assertEquals("error.fcm.receiver.invalid", e.getCode());
            assertEquals("Receiver list - no appUserId specified", e.getMessage());
        }
    }

    @Test
    void shouldThrowAnExceptionIfNoMessageProvided() {
        try {
            firebaseService.sendPushNotification(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Message is not specified", e.getMessage());
        }
    }

    @SneakyThrows
    private SendResponse newSendResponse(String messageId) {
        Method method = SendResponse.class.getDeclaredMethod("fromMessageId", String.class);
        method.setAccessible(true);
        return (SendResponse) method.invoke(SendResponse.class, messageId);
    }

    @SneakyThrows
    private SendResponse newFailedSendResponse() {
        Method method = SendResponse.class.getDeclaredMethod("fromException", FirebaseMessagingException.class);
        method.setAccessible(true);

        Method ex = FirebaseMessagingException.class.getDeclaredMethod(
            "withMessagingErrorCode", FirebaseException.class, MessagingErrorCode.class);
        ex.setAccessible(true);

        FirebaseMessagingException exInst = (FirebaseMessagingException)
            ex.invoke(FirebaseMessagingException.class, new FirebaseException(ErrorCode.INVALID_ARGUMENT, "msg", null), MessagingErrorCode.UNREGISTERED);
        return (SendResponse) method.invoke(SendResponse.class, exInst);
    }

    @SneakyThrows
    private <T> Object extractField(String name, T instance) {
        Field data = ReflectionUtils.findRequiredField(instance.getClass(), name);
        data.setAccessible(true);
        return data.get(instance);
    }

    @Data
    @Builder
    private static class TestBatchResponse implements BatchResponse {


        private List<SendResponse> responses;
        private int successCount;
        private int failureCount;
    }
}