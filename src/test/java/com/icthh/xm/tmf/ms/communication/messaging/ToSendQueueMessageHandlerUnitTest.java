package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.tmf.ms.communication.lep.LepKafkaMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.BasicMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
public class ToSendQueueMessageHandlerUnitTest {

    private static final String MESSAGE = "{\"type\":\"SMS\",\"content\":\"hello\","
        + "\"characteristic\":[{\"name\":\"TENANT.NAME\",\"value\":\"DEMO\"}]}";

    @Mock
    private MessageHandlerService messageHandlerService;
    @Mock
    private LepKafkaMessageHandler lepMessageHandler;
    @Mock
    private BasicMessageHandler basicMessageHandler;

    private ToSendQueueMessageHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new ToSendQueueMessageHandler(JsonMapper.builder().build(), messageHandlerService, lepMessageHandler);
    }

    @Test
    public void shouldHandleMessageInTenantFromCharacteristic() {
        when(messageHandlerService.getHandler("SMS")).thenReturn(basicMessageHandler);

        handler.onMessage(MESSAGE, "XM", new TopicConfig(), Map.of());

        verify(lepMessageHandler).preHandler("DEMO");
        CommunicationMessage handled = captureHandledMessage();
        assertThat(handled.getContent()).isEqualTo("hello");
        assertThat(findCharacteristic(handled, MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)).isEmpty();
        verify(lepMessageHandler).destroy();
    }

    @Test
    public void shouldHandleJsonStringWrappedInQuotes() {
        when(messageHandlerService.getHandler("SMS")).thenReturn(basicMessageHandler);

        handler.onMessage("\"" + MESSAGE + "\"", "XM", new TopicConfig(), Map.of());

        assertThat(captureHandledMessage().getContent()).isEqualTo("hello");
    }

    @Test
    public void shouldMoveRecordTimestampToCharacteristic() {
        when(messageHandlerService.getHandler("SMS")).thenReturn(basicMessageHandler);

        handler.onMessage(MESSAGE, "XM", new TopicConfig(),
            Map.of(KafkaHeaders.RECEIVED_TIMESTAMP, "1700000000123".getBytes(UTF_8)));

        assertThat(findCharacteristic(captureHandledMessage(), MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP))
            .contains("1700000000123");
    }

    @Test
    public void shouldNotRethrowHandlingErrorToAvoidResending() {
        when(messageHandlerService.getHandler("SMS")).thenReturn(basicMessageHandler);
        doThrow(new IllegalStateException("smpp is down")).when(basicMessageHandler).handle(any(CommunicationMessage.class));

        handler.onMessage(MESSAGE, "XM", new TopicConfig(), Map.of());

        verify(lepMessageHandler).destroy();
    }

    @Test
    public void shouldNotRethrowParsingError() {
        handler.onMessage("not a json", "XM", new TopicConfig(), Map.of());

        verify(lepMessageHandler, never()).preHandler(any());
        verify(lepMessageHandler).destroy();
    }

    private CommunicationMessage captureHandledMessage() {
        ArgumentCaptor<CommunicationMessage> captor = ArgumentCaptor.forClass(CommunicationMessage.class);
        verify(basicMessageHandler).handle(captor.capture());
        return captor.getValue();
    }

    private static Optional<String> findCharacteristic(CommunicationMessage message, String name) {
        return message.getCharacteristic().stream()
            .filter(it -> name.equals(it.getName()))
            .map(CommunicationRequestCharacteristic::getValue)
            .findFirst();
    }
}
