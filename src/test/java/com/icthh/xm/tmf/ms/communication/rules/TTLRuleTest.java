package com.icthh.xm.tmf.ms.communication.rules;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.FAILED;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.SUCCESS;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandler.ERROR_BUSINESS_RULE_VALIDATION;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.*;
import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP;
import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.TTL_EXCEED_MESSAGE_CODE;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper;
import com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandler;
import com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule;
import com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRuleConfig;
import com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRuleConfigService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TTLRuleConfigService.class})
public class TTLRuleTest {

    private static final String UPDATED_KEY = "/config/tenants/xm/tenant-config.yml";

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
    private TTLRuleConfigService ttlRuleConfigService;

    private SmppMessagingHandler smppMessagingHandler;

    @Test
    public void activeConfigTest() throws Throwable {
        TTLRuleConfig ttlRuleConfig = load30minConfig();

        assertEquals(new Long(30), ttlRuleConfig.getTtlRule().getValue());
        assertEquals(ChronoUnit.MINUTES.name(), ttlRuleConfig.getTtlRule().getChronoUnit());
        assertEquals(Duration.ofMinutes(30L), ttlRuleConfig.getTTL().get());
        assertEquals(TTLRuleConfig.Action.REJECT, ttlRuleConfig.getAction());
        assertTrue(ttlRuleConfig.isActive());
    }

    @Test
    public void inactiveConfigTest() throws Throwable {
        TTLRuleConfig ttlRuleConfig = loadInactiveConfig();

        assertEquals(new Long(1), ttlRuleConfig.getTtlRule().getValue());
        assertEquals(ChronoUnit.MILLIS.name(), ttlRuleConfig.getTtlRule().getChronoUnit());
        assertEquals(TTLRuleConfig.Action.NONE, ttlRuleConfig.getAction());
        assertFalse(ttlRuleConfig.isActive());
    }

    @Test
    public void warningConfigTest() throws Throwable {
        TTLRuleConfig ttlRuleConfig = loadWarningConfig();

        assertEquals(new Long(1), ttlRuleConfig.getTtlRule().getValue());
        assertEquals(ChronoUnit.MILLIS.name(), ttlRuleConfig.getTtlRule().getChronoUnit());
        assertEquals(TTLRuleConfig.Action.WARNING, ttlRuleConfig.getAction());
        assertTrue(ttlRuleConfig.isActive());
    }

    @Test
    public void defaultActionConfigTest() throws Throwable {
        TTLRuleConfig ttlRuleConfig = loadDefaultActionConfig();

        assertEquals(new Long(1), ttlRuleConfig.getTtlRule().getValue());
        assertEquals(ChronoUnit.MILLIS.name(), ttlRuleConfig.getTtlRule().getChronoUnit());
        assertEquals(TTLRuleConfig.Action.getDefaultValue(), ttlRuleConfig.getAction());
        assertFalse(ttlRuleConfig.isActive());
    }

    @Test
    public void noConfigTest() throws Throwable {
        TTLRuleConfig ttlRuleConfig = loadNoConfig();

        assertNull(ttlRuleConfig.getTtlRule());
        assertFalse(ttlRuleConfig.isActive());
    }

    @Test
    public void validateMessageSuccessBornTimeTest() throws Throwable {
        load30minConfig();

        successCheck(message().addCharacteristicItem(
            new CommunicationRequestCharacteristic()
                .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                .value(String.valueOf(Instant.now().toEpochMilli()))
        ));
    }

    @Test
    public void validateInactiveConfigTest() throws Throwable {
        loadInactiveConfig();

        successCheck(message().addCharacteristicItem(
            new CommunicationRequestCharacteristic()
                .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                .value(String.valueOf(Instant.now().toEpochMilli()))
        ));
    }

    @Test
    public void validateDefaultActionConfigTest() throws Throwable {
        loadDefaultActionConfig();

        successCheck(message().addCharacteristicItem(
            new CommunicationRequestCharacteristic()
                .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                .value(String.valueOf(Instant.now().toEpochMilli()))
        ));
    }

    @Test
    public void validateNoConfigTest() throws Throwable {
        loadNoConfig();

        successCheck(message().addCharacteristicItem(
            new CommunicationRequestCharacteristic()
                .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                .value(String.valueOf(Instant.now().toEpochMilli()))
        ));
    }

    /**
     * Test that outdated message is not rejected
     */
    @Test
    public void validateWarningTest() throws Throwable {
        loadWarningConfig();

        successCheck(message().addCharacteristicItem(
            new CommunicationRequestCharacteristic()
                .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                .value(String.valueOf(Instant.now().toEpochMilli()))
        ));
    }

    @Test
    public void validateMessageOutdatedBornTimeTest() throws Throwable {
        loadOneMillisecondConfig();

        failureCheck(message().addCharacteristicItem(
            new CommunicationRequestCharacteristic()
                .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                .value(String.valueOf(Instant.now().toEpochMilli()))
        ));
    }

    private TTLRuleConfig refreshConfig(String configFile) throws IOException {
        ttlRuleConfigService.onRefresh(UPDATED_KEY, IOUtils.toString(
            requireNonNull(getClass().getClassLoader().getResourceAsStream(configFile)),
            defaultCharset()));
        TTLRule ttlRule = new TTLRule(ttlRuleConfigService);
        BusinessRuleValidator businessRuleValidator = new BusinessRuleValidator(singletonList(ttlRule));
        smppMessagingHandler = new SmppMessagingHandler(kafkaTemplate,
            smppService,
            createApplicationProperties(),
            businessRuleValidator, mapper);

        return ttlRuleConfigService.getTtlRuleConfig();
    }

    private TTLRuleConfig load30minConfig() throws IOException {
        return refreshConfig("ttl30minConfig.yml");
    }

    private TTLRuleConfig loadInactiveConfig() throws IOException {
        return refreshConfig("ttlInaciveConfig.yml");
    }

    private TTLRuleConfig loadWarningConfig() throws IOException {
        return refreshConfig("ttlWarningConfig.yml");
    }

    private TTLRuleConfig loadDefaultActionConfig() throws IOException {
        return refreshConfig("ttlDefaultActionConfig.yml");
    }

    private TTLRuleConfig loadNoConfig() throws IOException {
        return refreshConfig("ttlNoConfig.yml");
    }

    private TTLRuleConfig loadOneMillisecondConfig() throws IOException {
        return refreshConfig("ttlOneMillisecondConfig.yml");
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

    private void failureCheck(CommunicationMessage message) {
        smppMessagingHandler.handle(message);
        MessageResponse messageResponse = new MessageResponse(FAILED, message);
        messageResponse.setErrorCode(TTL_EXCEED_MESSAGE_CODE);
        messageResponse.setErrorMessage(ERROR_BUSINESS_RULE_VALIDATION);
        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(FAIL_SEND), argumentCaptor.capture());
        verify(kafkaTemplate, never()).send(eq(SUCCESS_SENT), argumentCaptor.capture());
        MessageResponse payload = argumentCaptor.getValue();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        assertThat(payload, equalTo(messageResponse));
    }
}
