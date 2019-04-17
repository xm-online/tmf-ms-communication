package com.icthh.xm.tmf.ms.communication.messaging;

import static com.google.common.collect.ImmutableMap.*;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.SUCCESS;
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

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.rules.BusinessTimeRule;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;

@RunWith(MockitoJUnitRunner.class)
public class BusinessTimeRuleTest {

    private final static LocalDate LOCAL_DATE = LocalDate.of(2019, 04, 15);

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SmppService smppService;

    @Mock
    private TenantConfigService tenantConfigService;

    @Mock
    private BusinessRuleValidator businessRuleValidator;

    @Mock
    Clock clock;

    private Clock fixedClock;

    private MessagingHandler messagingHandler;
    private ApplicationProperties applicationProperties;

    @Before
    public void setUp() {

        fixedClock = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        when(tenantConfigService.getConfig()).thenReturn(createTenantConfig());

        applicationProperties = createApplicationProperties();
        BusinessTimeRule businessTimeRule = new BusinessTimeRule(tenantConfigService, clock);
        businessRuleValidator = new BusinessRuleValidator(singletonList(businessTimeRule));

        messagingHandler = new MessagingHandler(kafkaTemplate, smppService, applicationProperties,
            businessRuleValidator);
    }

    @Test
    public void receiveMessageSuccessTest() {
        messagingHandler.receiveMessage(message());

        MessageResponse messageResponse = new MessageResponse(SUCCESS, message());

        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), argumentCaptor.capture());
        MessageResponse payload = argumentCaptor.getValue();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        assertThat(payload, equalTo(messageResponse));
    }

    private Map<String, Object> createTenantConfig() {
        Map<String, Object> tenantConfig = new HashMap<>();
        tenantConfig.put("monday", of("startTime", "00:00:00", "endTime", "23:59:59"));
        tenantConfig.put("2019-03-17", of("startTime", "00:00:00", "endTime", "23:59:59"));

        return tenantConfig;
    }

}
