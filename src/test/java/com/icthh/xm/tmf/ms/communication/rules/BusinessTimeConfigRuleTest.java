package com.icthh.xm.tmf.ms.communication.rules;

import static com.google.common.collect.ImmutableMap.of;
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
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeRule;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.time.Clock;
import java.time.Instant;
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
    private BusinessTimeConfigService tenantConfigService;

    @Mock
    private Clock clock;

    private MessagingHandler messagingHandler;

    @Before
    public void setUp() {
        when(tenantConfigService.getConfig()).thenReturn(createTenantConfig());
        BusinessTimeRule businessTimeRule = new BusinessTimeRule(tenantConfigService, clock);

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
        messageResponse.setErrorCode("BusinessException");
        messageResponse.setErrorMessage("error.business.sending.notBusinessTime");
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

    private Map<String, Object> createTenantConfig() {
        Map<String, Object> businessTimeConfig = new HashMap<>();
        Map<String, Object> exceptionDayConfig = new HashMap<>();
        exceptionDayConfig.put(EXCEPTION_DATE, of("startTime", "13:00:00", "endTime", "15:30:00"));
        Map<String, Object> businessDay = new HashMap<>();
        businessDay.put("monday", of("startTime", "08:30:00", "endTime", "12:30:30"));
        businessTimeConfig.put("exceptionDate", exceptionDayConfig);
        businessTimeConfig.put("businessDay", businessDay);

        return of("businessDayConfig", businessTimeConfig);
    }

}
