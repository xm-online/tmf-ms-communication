package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.DELIVERY_REPORT;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.DESTINATION_TYPE_TON;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.ERROR_CODE;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.MESSAGE_ID;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.OPTIONAL_PARAMETER_PREFIX;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.PROTOCOL_ID;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.SOURCE_TYPE_TON;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.VALIDITY_PERIOD;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.BusinessRule;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.messaging.handler.AbstractSmppMessageHandler.ErrorCodes;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService.CustomParametersBuilder;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class AbstractSmppMessageHandlerUnitTest {

    public static final String SUCCESS_SENT = "success_sent";
    public static final String FAIL_SEND = "fail_send";
    public static final String TO_SEND = "to_send";
    public static final String SUCCESS_DELIVERY = "delivery_success";
    public static final String FAILED_DELIVERY = "delivery_failed";
    public static final String MO_QUEUE = "MO-QUEUE";

    private static final String SMSC_MESSAGE_ID = "test-message-id";
    private static final String PHONE_NUMBER = "380500000000";
    private static final String SENDER_ID = "TestSender";
    private static final String RECEIVER_ID = "ID";
    private static final String MESSAGE_CONTENT = "TestContent";
    private static final String MESSAGE_TYPE_SMS = "SMS";
    private static final String DISTRIBUTION_ID_KEY = "DISTRIBUTION.ID";
    private static final String DISTRIBUTION_ID_VALUE = "TEST_D_ID";
    private static final String RULE_ERROR_CODE = "error.rule";
    private static final String DELIVERY_REPORT_VALUE = "1";
    private static final int NEGATIVE_RESPONSE_CODE = 20;
    private static final String BUSINESS_EXCEPTION_CODE = "bizCode";
    private static final String OPTIONAL_TAG = "6005";
    private static final short OPTIONAL_TAG_SHORT = 6005;
    private static final String OPTIONAL_VALUE = "30001";
    private static final int PROTOCOL_ID_VALUE = 68;
    private static final int VALIDITY_PERIOD_VALUE = 120;

    protected static final List<CommunicationRequestCharacteristic> CHARACTERISTICS = List.of(
        new CommunicationRequestCharacteristic().name(DELIVERY_REPORT).value(DELIVERY_REPORT_VALUE)
    );

    private static final ApplicationProperties APPLICATION_PROPERTIES = createApplicationProperties();

    private AbstractSmppMessageHandler unit;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private BusinessRuleValidator businessRuleValidator;
    @Mock
    private CommunicationMessageMapper mapper;

    @BeforeEach
    void setUp() {
        unit = spy(new AbstractSmppMessageHandler(kafkaTemplate, APPLICATION_PROPERTIES,
            businessRuleValidator, mapper) {
            @Override
            protected String doSend(CommunicationMessage message, String phoneNumber) throws Exception {
                return SMSC_MESSAGE_ID;
            }

            @Override
            public MessageType getType() {
                return null;
            }
        });
        RuleResponse ok = new RuleResponse();
        ok.setSuccess(true);
        lenient().when(businessRuleValidator.validate(any())).thenReturn(ok);
    }

    static Stream<Arguments> smppExceptions() {
        return Stream.of(
            Arguments.of(new NegativeResponseException(NEGATIVE_RESPONSE_CODE), String.valueOf(NEGATIVE_RESPONSE_CODE)),
            Arguments.of(new InvalidResponseException("error"), ErrorCodes.ERROR_SYSTEM_SENDING_INVALID_RESPONSE),
            Arguments.of(new ResponseTimeoutException("error"), ErrorCodes.ERROR_SYSTEM_SENDING_RESPONSE_TIMEOUT),
            Arguments.of(new PDUException("error"), ErrorCodes.ERROR_SYSTEM_SENDING_PDU),
            Arguments.of(new BusinessException(BUSINESS_EXCEPTION_CODE, "error"), BUSINESS_EXCEPTION_CODE),
            Arguments.of(new RuntimeException("error"), ErrorCodes.ERROR_SYSTEM_GENERAL_INTERNAL_SERVER_ERROR)
        );
    }

    @ParameterizedTest(name = "[{index}] {0} → errorCode={1}")
    @MethodSource("smppExceptions")
    void shouldPublishToFailQueueAndSetErrorCode(Exception exception, String expectedErrorCode) throws Exception {
        CommunicationMessage msg = message();
        doThrow(exception).when(unit).doSend(msg, PHONE_NUMBER);

        unit.handle(msg);

        ArgumentCaptor<MessageResponse> captor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(FAIL_SEND), captor.capture());
        assertEquals(Status.FAILED, captor.getValue().getStatus());
        assertEquals(expectedErrorCode, findCharacteristic(msg, ERROR_CODE));
    }

    @Test
    void shouldPublishToSuccessQueue() {
        CommunicationMessage msg = message();
        unit.handle(msg);

        ArgumentCaptor<MessageResponse> captor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), captor.capture());
        assertEquals(Status.SUCCESS, captor.getValue().getStatus());
        assertEquals(SMSC_MESSAGE_ID, findCharacteristic(msg, MESSAGE_ID));
    }

    @Test
    void shouldHandleNullCharacteristics() {
        CommunicationMessage msg = message();
        msg.setCharacteristic(null);
        unit.handle(msg);

        ArgumentCaptor<MessageResponse> captor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), captor.capture());
        assertEquals(Status.SUCCESS, captor.getValue().getStatus());
    }

    @Test
    void shouldSetDistributionIdInResponse() {
        CommunicationMessage msg = message();
        msg.setCharacteristic(new ArrayList<>());
        msg.getCharacteristic().add(
            new CommunicationRequestCharacteristic().name(DISTRIBUTION_ID_KEY).value(DISTRIBUTION_ID_VALUE));
        unit.handle(msg);

        ArgumentCaptor<MessageResponse> captor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), captor.capture());
        assertEquals(DISTRIBUTION_ID_VALUE, captor.getValue().getDistributionId());
    }

    @Test
    void shouldPublishToFailQueueWhenBusinessRuleRejects() {
        RuleResponse fail = new RuleResponse();
        fail.setSuccess(false);
        fail.setResponseCode(RULE_ERROR_CODE);
        when(businessRuleValidator.validate(any())).thenReturn(fail);

        CommunicationMessage msg = message();
        unit.handle(msg);

        ArgumentCaptor<MessageResponse> captor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(FAIL_SEND), captor.capture());
        assertEquals(Status.FAILED, captor.getValue().getStatus());
        assertEquals(RULE_ERROR_CODE, findCharacteristic(msg, ERROR_CODE));
    }

    @Test
    void shouldDelegateMessageCreateToHandle() {
        CommunicationMessageCreate create = messageCreate();
        CommunicationMessage mapped = message();
        when(mapper.messageCreateToMessage(create)).thenReturn(mapped);

        CommunicationMessage result = unit.handle(create);

        assertNotNull(result);
        verify(mapper).messageCreateToMessage(create);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), any());
    }

    @Test
    void shouldParseDeliveryReportCharacteristic() {
        assertEquals((byte) 1, unit.getDeliveryReport(characteristicList(DELIVERY_REPORT, "1")));
        assertEquals((byte) 0, unit.getDeliveryReport(characteristicList(DELIVERY_REPORT, "0")));
        assertEquals((byte) 9, unit.getDeliveryReport(characteristicList(DELIVERY_REPORT, "9")));
        assertEquals((byte) 0, unit.getDeliveryReport(characteristicList(DELIVERY_REPORT, "invalid")));
    }

    @Test
    void shouldBuildCustomParametersWithAllFields() {
        List<CommunicationRequestCharacteristic> chars = new ArrayList<>();
        chars.add(new CommunicationRequestCharacteristic().name(SOURCE_TYPE_TON).value("INTERNATIONAL"));
        chars.add(new CommunicationRequestCharacteristic().name(DESTINATION_TYPE_TON).value("NATIONAL"));
        chars.add(new CommunicationRequestCharacteristic().name(PROTOCOL_ID).value(String.valueOf(PROTOCOL_ID_VALUE)));
        chars.add(new CommunicationRequestCharacteristic().name(VALIDITY_PERIOD)
            .value(String.valueOf(VALIDITY_PERIOD_VALUE)));

        CustomParametersBuilder result = unit.buildCustomParameters(chars);

        assertEquals("INTERNATIONAL", result.getSourceTon());
        assertEquals("NATIONAL", result.getDestinationTon());
        assertEquals(PROTOCOL_ID_VALUE, result.getProtocolId().intValue());
        assertEquals(VALIDITY_PERIOD_VALUE, result.getValidityPeriod().intValue());
    }

    @Test
    void shouldReturnNullFieldsWhenCharacteristicsEmpty() {
        CustomParametersBuilder result = unit.buildCustomParameters(List.of());

        assertNull(result.getSourceTon());
        assertNull(result.getDestinationTon());
        assertNull(result.getProtocolId());
        assertNull(result.getValidityPeriod());
    }

    @Test
    void shouldReturnNullFieldsWhenCharacteristicsNull() {
        CustomParametersBuilder result = unit.buildCustomParameters(null);

        assertNull(result.getSourceTon());
        assertNull(result.getDestinationTon());
        assertNull(result.getProtocolId());
        assertNull(result.getValidityPeriod());
    }

    @Test
    void shouldReturnNullProtocolIdAndValidityPeriodWhenValueNotParseable() {
        List<CommunicationRequestCharacteristic> chars = new ArrayList<>();
        chars.add(new CommunicationRequestCharacteristic().name(PROTOCOL_ID).value("not-a-number"));
        chars.add(new CommunicationRequestCharacteristic().name(VALIDITY_PERIOD).value("not-a-number"));

        CustomParametersBuilder result = unit.buildCustomParameters(chars);

        assertNull(result.getProtocolId());
        assertNull(result.getValidityPeriod());
    }

    @Test
    void shouldBuildOptionalParametersFromCharacteristics() {
        CommunicationMessage msg = message();
        msg.getCharacteristic().add(
            new CommunicationRequestCharacteristic()
                .name(OPTIONAL_PARAMETER_PREFIX + OPTIONAL_TAG).value(OPTIONAL_VALUE));

        Map<Short, String> result = unit.buildOptionalParameters(msg);
        assertEquals(Map.of(OPTIONAL_TAG_SHORT, OPTIONAL_VALUE), result);
    }

    @Test
    void shouldReturnEmptyOptionalParametersWhenNone() {
        CommunicationMessage msg = message();
        Map<Short, String> result = unit.buildOptionalParameters(msg);
        assertEquals(Map.of(), result);
    }

    public static ApplicationProperties createApplicationProperties() {
        ApplicationProperties props = new ApplicationProperties();
        Messaging messaging = new Messaging();
        props.setMessaging(messaging);
        messaging.setToSendQueueName(TO_SEND);
        messaging.setSentQueueName(SUCCESS_SENT);
        messaging.setSendFailedQueueName(FAIL_SEND);
        messaging.setDeliveryFailedQueueName(FAILED_DELIVERY);
        messaging.setDeliveredQueueName(SUCCESS_DELIVERY);
        messaging.setDeliveredMoQueueName(MO_QUEUE);
        BusinessRule businessRule = new BusinessRule();
        businessRule.setEnableBusinessTimeRule(false);
        props.setBusinessRule(businessRule);
        return props;
    }

    public static CommunicationMessage message() {
        CommunicationMessage msg = new CommunicationMessage();
        Receiver receiver = new Receiver();
        receiver.setPhoneNumber(PHONE_NUMBER);
        receiver.setId(RECEIVER_ID);
        Sender sender = new Sender();
        sender.setId(SENDER_ID);
        msg.setId(SMSC_MESSAGE_ID);
        msg.setSender(sender);
        msg.setContent(MESSAGE_CONTENT);
        msg.setReceiver(singletonList(receiver));
        msg.setType(MESSAGE_TYPE_SMS);
        msg.setCharacteristic(Lists.newArrayList(
            new CommunicationRequestCharacteristic().name(DELIVERY_REPORT).value(DELIVERY_REPORT_VALUE)));
        return msg;
    }

    public static CommunicationMessage message(String deliveryValue) {
        CommunicationMessage msg = new CommunicationMessage();
        msg.setType(MESSAGE_TYPE_SMS);
        msg.setCharacteristic(Lists.newArrayList(
            new CommunicationRequestCharacteristic().name(DELIVERY_REPORT).value(deliveryValue)));
        return msg;
    }

    public static CommunicationMessageCreate messageCreate() {
        CommunicationMessageCreate msg = new CommunicationMessageCreate();
        Receiver receiver = new Receiver();
        receiver.setPhoneNumber(PHONE_NUMBER);
        receiver.setId(RECEIVER_ID);
        Sender sender = new Sender();
        sender.setId(SENDER_ID);
        msg.setSender(sender);
        msg.setContent(MESSAGE_CONTENT);
        msg.setReceiver(singletonList(receiver));
        msg.setType(MESSAGE_TYPE_SMS);
        msg.setCharacteristic(CHARACTERISTICS);
        return msg;
    }

    private static List<CommunicationRequestCharacteristic> characteristicList(String name, String value) {
        return Lists.newArrayList(new CommunicationRequestCharacteristic().name(name).value(value));
    }

    static String findCharacteristic(CommunicationMessage msg, String name) {
        return msg.getCharacteristic().stream()
            .filter(c -> name.equals(c.getName()))
            .findFirst()
            .map(CommunicationRequestCharacteristic::getValue)
            .orElse(null);
    }
}

