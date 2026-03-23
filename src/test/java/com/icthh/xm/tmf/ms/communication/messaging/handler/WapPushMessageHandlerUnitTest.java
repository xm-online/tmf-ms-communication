package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.AbstractSmppMessageHandlerUnitTest.createApplicationProperties;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.DELIVERY_REPORT;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.service.SmppService.CustomParametersBuilder;
import com.icthh.xm.tmf.ms.communication.service.WapPushSegmentationService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.shaded.com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
class WapPushMessageHandlerUnitTest {

    private static final String PHONE_NUMBER = "380500000000";
    private static final String SENDER_ID = "TestSender";
    private static final String HEX_CONTENT = "CD".repeat(10);
    private static final String DELIVERY_REPORT_VALUE = "1";
    private static final byte EXPECTED_DELIVERY_REPORT = 1;
    private static final String MSG_ID_1 = "smsc-id-1";
    private static final String MSG_ID_2 = "smsc-id-2";
    private static final byte[] SEGMENT_1 = {1, 2, 3};
    private static final byte[] SEGMENT_2 = {4, 5, 6};
    private static final List<byte[]> SEGMENTS = List.of(SEGMENT_1, SEGMENT_2);

    private WapPushMessageHandler handler;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private SmppService smppService;
    @Mock
    private WapPushSegmentationService segmentationService;
    @Mock
    private BusinessRuleValidator businessRuleValidator;
    @Mock
    private CommunicationMessageMapper mapper;

    @BeforeEach
    void setUp() {
        handler = spy(new WapPushMessageHandler(
            kafkaTemplate, smppService, segmentationService,
            createApplicationProperties(), businessRuleValidator, mapper));
    }

    @Test
    void shouldReturnWapPushType() {
        assertEquals(MessageType.WapPush, handler.getType());
    }

    @Test
    void shouldSendAllSegmentsAndJoinMessageIds() throws Exception {
        when(segmentationService.buildWapPushSegmentDetails(HEX_CONTENT)).thenReturn(SEGMENTS);
        when(smppService.sendBinary(eq(PHONE_NUMBER), eq(SENDER_ID), eq(EXPECTED_DELIVERY_REPORT),
            eq(SEGMENT_1), any(CustomParametersBuilder.class), anyMap())).thenReturn(MSG_ID_1);
        when(smppService.sendBinary(eq(PHONE_NUMBER), eq(SENDER_ID), eq(EXPECTED_DELIVERY_REPORT),
            eq(SEGMENT_2), any(CustomParametersBuilder.class), anyMap())).thenReturn(MSG_ID_2);

        CommunicationMessage msg = wapPushMessage(HEX_CONTENT);

        String result = handler.doSend(msg, PHONE_NUMBER);

        assertEquals(MSG_ID_1 + "," + MSG_ID_2, result);
        verify(smppService, times(2)).sendBinary(
            anyString(), anyString(), anyByte(), any(byte[].class),
            any(CustomParametersBuilder.class), anyMap());
    }

    @Test
    void shouldSendSingleSegment() throws Exception {
        List<byte[]> singleSegment = List.of(SEGMENT_1);
        when(segmentationService.buildWapPushSegmentDetails(HEX_CONTENT)).thenReturn(singleSegment);
        when(smppService.sendBinary(eq(PHONE_NUMBER), eq(SENDER_ID), eq(EXPECTED_DELIVERY_REPORT),
            eq(SEGMENT_1), any(CustomParametersBuilder.class), anyMap())).thenReturn(MSG_ID_1);

        CommunicationMessage msg = wapPushMessage(HEX_CONTENT);

        String result = handler.doSend(msg, PHONE_NUMBER);

        assertEquals(MSG_ID_1, result);
        verify(smppService, times(1)).sendBinary(
            anyString(), anyString(), anyByte(), any(byte[].class),
            any(CustomParametersBuilder.class), anyMap());
    }

    @Test
    void shouldThrowWhenContentIsNull() throws Exception {
        CommunicationMessage msg = wapPushMessage(null);
        assertThrows(IllegalArgumentException.class, () -> handler.doSend(msg, PHONE_NUMBER));
        verify(smppService, never()).sendBinary(
            anyString(), anyString(), anyByte(), any(byte[].class),
            any(CustomParametersBuilder.class), anyMap());
    }

    @Test
    void shouldThrowWhenContentIsBlank() throws Exception {
        CommunicationMessage msg = wapPushMessage("   ");
        assertThrows(IllegalArgumentException.class, () -> handler.doSend(msg, PHONE_NUMBER));
        verify(smppService, never()).sendBinary(
            anyString(), anyString(), anyByte(), any(byte[].class),
            any(CustomParametersBuilder.class), anyMap());
    }

    private static CommunicationMessage wapPushMessage(String hexContent) {
        CommunicationMessage msg = new CommunicationMessage();
        Receiver receiver = new Receiver();
        receiver.setPhoneNumber(PHONE_NUMBER);
        Sender sender = new Sender();
        sender.setId(SENDER_ID);
        msg.setSender(sender);
        msg.setContent(hexContent);
        msg.setReceiver(singletonList(receiver));
        msg.setType(MessageType.WapPush.toString());
        msg.setCharacteristic(Lists.newArrayList(
            new CommunicationRequestCharacteristic().name(DELIVERY_REPORT).value(DELIVERY_REPORT_VALUE)));
        return msg;
    }
}
