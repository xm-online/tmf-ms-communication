package com.icthh.xm.tmf.ms.communication.service;

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

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.channel.mobileapp.FirebaseApplicationConfigurationProvider;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.util.ReflectionUtils;


public class FirebaseServiceTest {
    public static final String SENDER_ID = "sender-id";
    public static final String TENANT_KEY = "tenant-key";
    public static final String FCM_MESSAGE_ID = "fcm-message-id";
    public static final String TEST_CONTENT = "test-content";
    public static final String TEST_SUBJECT = "test-subject";
    public static final String RECEIVER_ID = "receiver-id";
    public static final String APP_USER_ID = "app-user-id";
    public static final String CUSTOM_NAME = "custom-name";
    public static final String CUSTOM_VALUE = "custom-value";
    public static final String CUSTOMIZED_KEY = "customized-key";
    public static final String CUSTOMIZED_VALUE = "customized-value";

    private TenantContextHolder tenantContextHolder = mock(TenantContextHolder.class);
    private FirebaseApplicationConfigurationProvider configurationProvider =
        mock(FirebaseApplicationConfigurationProvider.class);
    private MobileAppMessagePayloadCustomizationService payloadCustomizationServiceMock =
        mock(MobileAppMessagePayloadCustomizationService.class);

    private FirebaseService firebaseService = new FirebaseService(configurationProvider,
        tenantContextHolder, payloadCustomizationServiceMock);

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
                .responses(List.of(newSendResponse(FCM_MESSAGE_ID)))
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
                    .id(RECEIVER_ID)
                    .appUserId(APP_USER_ID)
                )
            )
            .characteristic(List.of(
                new CommunicationRequestCharacteristic()
                    .name(CUSTOM_NAME)
                    .value(CUSTOM_VALUE)
            ));

        //when:
        CommunicationMessage result = firebaseService.sendPushNotification(message);

        //then:
        assertNotNull(result);
        List<CommunicationRequestCharacteristic> characteristic = result.getCharacteristic();
        assertEquals(new CommunicationRequestCharacteristic()
                .name("successCount").value("1"),
            characteristic.get(0));
        assertEquals(new CommunicationRequestCharacteristic()
                .name("failureCount").value("0"),
            characteristic.get(1));
        assertEquals(new CommunicationRequestCharacteristic()
                .name("messageId").value(FCM_MESSAGE_ID),
            characteristic.get(2));

        ArgumentCaptor<MulticastMessage> multicastCaptor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(messagingMock).sendMulticast(multicastCaptor.capture());
        verifyNoMoreInteractions(messagingMock);

        MulticastMessage multicastMessage = multicastCaptor.getValue();

        assertEquals(extractField("tokens", multicastMessage), List.of(APP_USER_ID));
        assertEquals(Map.of(CUSTOMIZED_KEY, CUSTOMIZED_VALUE, CUSTOM_NAME, CUSTOM_VALUE),
            extractField("data", multicastMessage));

        Notification notifications = (Notification) extractField("notification", multicastMessage);
        assertEquals(TEST_SUBJECT, extractField("title", notifications));
        assertEquals(TEST_CONTENT, extractField("body", notifications));
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
