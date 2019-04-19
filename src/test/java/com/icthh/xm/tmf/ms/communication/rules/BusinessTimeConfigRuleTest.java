package com.icthh.xm.tmf.ms.communication.rules;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.FAILED;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.SUCCESS;
import static com.icthh.xm.tmf.ms.communication.messaging.MessagingTest.FAIL_SEND;
import static com.icthh.xm.tmf.ms.communication.messaging.MessagingTest.SUCCESS_SENT;
import static com.icthh.xm.tmf.ms.communication.messaging.MessagingTest.createApplicationProperties;
import static com.icthh.xm.tmf.ms.communication.messaging.MessagingTest.message;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingHandler;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig.BusinessTime;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessDayConfig.BusinessTimeConfig;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeRule;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class BusinessTimeConfigRuleTest {

    private static final String BUSINESS_TIME = "2019-04-15T10:15:30.00Z";
    private static final String NOT_BUSINESS_TIME = "2019-04-15T02:15:30.00Z";
    private static final String EXCEPTION_DATE = "2019-03-17";
    private static final String EXCEPTION_DATE_BUSINESS_TIME = EXCEPTION_DATE + "T14:15:30.00Z";
    private static final String EXCEPTION_DATE_NOT_BUSINESS_TIME = EXCEPTION_DATE + "T16:15:30.00Z";
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SmppService smppService;

    @Mock
    private BusinessTimeConfigService businessTimeConfigService;

    @Mock
    private Clock clock;

    private MessagingHandler messagingHandler;

    @Before
    public void setUp() {
        when(businessTimeConfigService.getBusinessDayConfig()).thenReturn(createTenantConfig());
        BusinessTimeRule businessTimeRule = new BusinessTimeRule(businessTimeConfigService, clock);

        BusinessRuleValidator businessRuleValidator = new BusinessRuleValidator(singletonList(businessTimeRule));
        messagingHandler = new MessagingHandler(kafkaTemplate,
                                                smppService,
                                                createApplicationProperties(),
                                                businessRuleValidator);
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

    private void successCheck(CommunicationMessage message) {
        messagingHandler.receiveMessage(message);
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
        messagingHandler.receiveMessage(message());
        MessageResponse messageResponse = new MessageResponse(FAILED, message());
        messageResponse.setErrorCode("error.business.sending.notBusinessTime");
        messageResponse.setErrorMessage(MessagingHandler.ERROR_BUSINESS_RULE_VALIDATION);
        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(FAIL_SEND), argumentCaptor.capture());
        MessageResponse payload = argumentCaptor.getValue();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        assertThat(payload, equalTo(messageResponse));
    }

    private void configLocalTime(String localTime) {
        Clock fixedClock = Clock.fixed(Instant.parse(localTime), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
    }

    private BusinessDayConfig createTenantConfig() {
        Map<LocalDate, BusinessTime> exceptionDates = new HashMap<>();
        exceptionDates.put(LocalDate.parse(EXCEPTION_DATE),
                           new BusinessTime(LocalTime.parse("13:00:00"), LocalTime.parse("15:30:00")));

        Map<String, BusinessTime> businessDays = new HashMap<>();
        businessDays.put("monday", new BusinessTime(LocalTime.parse("08:30:00"), LocalTime.parse("12:30:30")));

        BusinessTimeConfig businessTimeConfig = new BusinessTimeConfig();
        businessTimeConfig.setBusinessDay(businessDays);
        businessTimeConfig.setExceptionDate(exceptionDates);

        BusinessDayConfig businessDayConfig = new BusinessDayConfig();
        businessDayConfig.setBusinessTime(businessTimeConfig);

        return businessDayConfig;
    }
}
