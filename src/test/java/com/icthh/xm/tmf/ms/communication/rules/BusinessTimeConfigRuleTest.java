package com.icthh.xm.tmf.ms.communication.rules;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.FAILED;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.SUCCESS;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandler.ERROR_BUSINESS_RULE_VALIDATION;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandler.ParameterNames.ERROR_CODE;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.FAIL_SEND;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.SUCCESS_SENT;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.createApplicationProperties;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.message;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDate.parse;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static java.time.LocalTime.of;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper;
import com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandler;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig.BusinessTime;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig.BusinessTimeConfig;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeRule;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {BusinessTimeConfigService.class})
public class BusinessTimeConfigRuleTest {

    private static final String BUSINESS_TIME = "2019-04-15T10:15:30.00Z";
    private static final String NOT_BUSINESS_TIME = "2019-04-15T02:15:30.00Z";
    private static final String EXCEPTION_DATE = "2019-03-17";
    private static final String EXCEPTION_DATE_BUSINESS_TIME = EXCEPTION_DATE + "T14:15:30.00Z";
    private static final String EXCEPTION_DATE_NOT_BUSINESS_TIME = EXCEPTION_DATE + "T16:15:30.00Z";
    private static final String UPDATED_KEY = "/config/tenants/xm/tenant-config.yml";

    //exceptionalCharacteristics
    private static final CommunicationRequestCharacteristic firstExceptionalCharacteristic =
        new CommunicationRequestCharacteristic().name("firstException").value("firstValue");

    private static final CommunicationRequestCharacteristic secondExceptionalCharacteristic =
        new CommunicationRequestCharacteristic().name("secondException").value("secondValue");

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SmppService smppService;

    @Mock
    private Clock clock;

    @Mock
    private CommunicationMessageMapper mapper;

    @MockBean
    private XmConfigProperties xmConfigProperties;

    @MockBean
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private BusinessTimeConfigService businessTimeConfigService;

    private SmppMessagingHandler smppMessagingHandler;

    @SneakyThrows
    @Before
    public void setUp() {
        businessTimeConfigService.onRefresh(UPDATED_KEY, IOUtils.toString(
            requireNonNull(getClass().getClassLoader().getResourceAsStream("businessTimeConfig.yml")),
            defaultCharset()));

        BusinessTimeRule businessTimeRule = new BusinessTimeRule(businessTimeConfigService, clock);
        BusinessRuleValidator businessRuleValidator = new BusinessRuleValidator(singletonList(businessTimeRule));
        smppMessagingHandler = new SmppMessagingHandler(kafkaTemplate,
                                                smppService,
                                                createApplicationProperties(),
                                                businessRuleValidator, mapper);
    }

    @Test
    public void getBusinessTimeConfigTest() {
        BusinessTimeConfig businessTimeConfig = businessTimeConfigService.getBusinessDayConfig()
                                                                         .getBusinessTime();

        BusinessTime mondayBusinessTime = businessTimeConfig.getBusinessDay().get("monday");
        assertEquals(mondayBusinessTime.getStartTime(), of(8, 30));
        assertEquals(mondayBusinessTime.getEndTime(), of(12, 30, 30));

        BusinessTime tuesdayBusinessTime = businessTimeConfig.getBusinessDay().get("tuesday");
        assertEquals(tuesdayBusinessTime.getEndTime(), MAX);

        BusinessTime wednesdayBusinessTime = businessTimeConfig.getBusinessDay().get("wednesday");
        assertEquals(wednesdayBusinessTime.getStartTime(), MIN);

        BusinessTime exceptionDateBusinessTime = businessTimeConfig.getExceptionDate().get(parse("2019-03-17"));
        assertEquals(exceptionDateBusinessTime.getStartTime(), of(13, 00));
        assertEquals(exceptionDateBusinessTime.getEndTime(), of(15, 30));

        List<CommunicationRequestCharacteristic> exceptionCharacteristics = businessTimeConfig.getExceptionCharacteristics();
        assertEquals(exceptionCharacteristics.size(), 2);
        assertTrue(exceptionCharacteristics.contains(firstExceptionalCharacteristic));
        assertTrue(exceptionCharacteristics.contains(secondExceptionalCharacteristic));
    }

    @Test
    public void validateMessageWithIdBusinessTimeTest() {
        configLocalTime(BUSINESS_TIME);
        successCheck(message());
    }

    @Test
    public void validateMessageNotBusinessTimeTest() {
        configLocalTime(NOT_BUSINESS_TIME);
        failureCheck();
    }

    @Test
    public void validateTimeMessageWithoutIdNotBusinessTimeTest() {
        configLocalTime(NOT_BUSINESS_TIME);
        successCheck(message().id(null));
    }

    @Test
    public void validateMessageExceptionDateTest() {
        configLocalTime(EXCEPTION_DATE_BUSINESS_TIME);
        successCheck(message());
    }

    @Test
    public void validateMessageExceptionDateNotBusinessTimeTest() {
        configLocalTime(EXCEPTION_DATE_NOT_BUSINESS_TIME);
        failureCheck();
    }

    @Test
    public void validateMessageNotBusinessTimeWithFirstExceptionTest() {
        configLocalTime(NOT_BUSINESS_TIME);
        successCheck(message().addCharacteristicItem(firstExceptionalCharacteristic));
    }

    @Test
    public void validateMessageNotBusinessTimeWithSecondExceptionTest() {
        configLocalTime(NOT_BUSINESS_TIME);
        successCheck(message().addCharacteristicItem(secondExceptionalCharacteristic));
    }


    private void successCheck(CommunicationMessage message) {
        smppMessagingHandler.handle(message);
        MessageResponse messageResponse = new MessageResponse(SUCCESS, message);
        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), argumentCaptor.capture());
        MessageResponse payload = argumentCaptor.getValue();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        assertThat(payload, equalTo(messageResponse));
    }

    private void failureCheck() {
        smppMessagingHandler.handle(message());
        MessageResponse messageResponse = new MessageResponse(FAILED, message());
        messageResponse.setErrorCode("error.business.sending.notBusinessTime");
        messageResponse.setErrorMessage(ERROR_BUSINESS_RULE_VALIDATION);
        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(FAIL_SEND), argumentCaptor.capture());
        verify(kafkaTemplate, never()).send(eq(SUCCESS_SENT), argumentCaptor.capture());
        MessageResponse payload = argumentCaptor.getValue();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        messageResponse.getResponseTo().getCharacteristic().add(new CommunicationRequestCharacteristic()
            .name(ERROR_CODE)
            .value("error.business.sending.notBusinessTime"));
        assertThat(payload, equalTo(messageResponse));
    }

    private void configLocalTime(String localTime) {
        Clock fixedClock = Clock.fixed(Instant.parse(localTime), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
    }
}
