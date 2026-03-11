package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.AbstractSmppMessageHandlerUnitTest.CHARACTERISTICS;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.AbstractSmppMessageHandlerUnitTest.createApplicationProperties;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.AbstractSmppMessageHandlerUnitTest.message;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.service.SmppService.CustomParametersBuilder;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
public class SmppMessagingHandlerUnitTest {

    public static final String SUCCESS_SENT = AbstractSmppMessageHandlerUnitTest.SUCCESS_SENT;
    public static final String FAIL_SEND = AbstractSmppMessageHandlerUnitTest.FAIL_SEND;

    private static final String SMSC_MESSAGE_ID = "smsc-msg-001";
    private static final String PHONE_NUMBER = "380500000000";
    private static final String SENDER_ID = "TestSender";
    private static final String MESSAGE_CONTENT = "TestContent";
    private static final String OPTIONAL_TAG = "6005";
    private static final short OPTIONAL_TAG_SHORT = 6005;
    private static final String OPTIONAL_VALUE = "30001";
    private static final byte EXPECTED_DELIVERY_REPORT = 1;
    private static final Map<Short, String> OPTIONAL_PARAMETERS = Map.of(OPTIONAL_TAG_SHORT, OPTIONAL_VALUE);

    private SmppMessagingHandler unit;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private SmppService smppService;
    @Mock
    private BusinessRuleValidator businessRuleValidator;
    @Mock
    private CommunicationMessageMapper mapper;

    @BeforeEach
    void setUp() {
        unit = spy(new SmppMessagingHandler(
            kafkaTemplate, smppService, createApplicationProperties(), businessRuleValidator, mapper));
    }

    @Test
    void shouldReturnSmsType() {
        assertEquals(MessageType.SMS, unit.getType());
    }

    @Test
    void shouldDelegateToSmppServiceSend() throws Exception {

        CommunicationMessage msg = message();
        CustomParametersBuilder customParametersBuilder = CustomParametersBuilder.builder().build();
        doReturn(customParametersBuilder).when(unit).buildCustomParameters(CHARACTERISTICS);
        doReturn(OPTIONAL_PARAMETERS).when(unit).buildOptionalParameters(msg);

        when(smppService.send(
            PHONE_NUMBER,
            MESSAGE_CONTENT,
            SENDER_ID,
            EXPECTED_DELIVERY_REPORT,
            OPTIONAL_PARAMETERS,
            customParametersBuilder))
            .thenReturn(SMSC_MESSAGE_ID);

        String result = unit.doSend(msg, PHONE_NUMBER);

        assertEquals(SMSC_MESSAGE_ID, result);
    }
}
