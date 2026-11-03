package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.unwrap;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.commons.topic.message.MessageHandler;
import com.icthh.xm.tmf.ms.communication.lep.LepKafkaMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.kafka.support.KafkaHeaders;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles messages from the {@code application.messaging.to-send-queue-name} topic.
 * <p>
 * Every error is logged and swallowed, so a message is never retried: a retry could send the SMS twice.
 */
@Slf4j
@RequiredArgsConstructor
public class ToSendQueueMessageHandler implements MessageHandler {

    public static final String TENANT_NAME = "TENANT.NAME";
    public static final String XM = "XM";

    private final ObjectMapper objectMapper;
    private final MessageHandlerService messageHandlerService;
    private final LepKafkaMessageHandler lepMessageHandler;

    @Override
    public void onMessage(String message, String tenant, TopicConfig topicConfig, Map<String, byte[]> headers) {
        final StopWatch stopWatch = StopWatch.createStarted();
        try {
            String payload = unwrap(message, "\"");
            log.info("start processing message, json body = {}", payload);
            CommunicationMessage communicationMessage = mapToCommunicationMessage(payload);
            lepMessageHandler.preHandler(getTenant(communicationMessage));
            addReceivedByChannelCharacteristic(communicationMessage, headers);
            messageHandlerService.getHandler(communicationMessage.getType())
                .handle(communicationMessage);
            log.info("stop processing message, time = {}", stopWatch.getTime());
        } catch (Exception e) {
            log.error("Error process event", e);
        } finally {
            lepMessageHandler.destroy();
        }
    }

    @SneakyThrows
    private CommunicationMessage mapToCommunicationMessage(String eventBody) {
        return objectMapper.readValue(eventBody, CommunicationMessage.class);
    }

    /**
     * Since Kafka headers are not accessible from the business rules,
     * move Kafka received timestamp to the communication message characteristics
     */
    private void addReceivedByChannelCharacteristic(CommunicationMessage communicationMessage,
                                                    Map<String, byte[]> headers) {
        Optional.ofNullable(headers)
            .map(it -> it.get(KafkaHeaders.RECEIVED_TIMESTAMP))
            .map(value -> new String(value, UTF_8))
            .filter(StringUtils::isNumeric)
            .ifPresent(kafkaReceivedTimestamp ->
                communicationMessage.addCharacteristicItem(
                    new CommunicationRequestCharacteristic()
                        // Rename it to unlink name from source channel
                        .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                        .value(kafkaReceivedTimestamp)
                )
            );
    }

    private String getTenant(CommunicationMessage message) {
        return message.getCharacteristic().stream().filter(ch -> TENANT_NAME.equals(ch.getName())).findFirst()
            .map(CommunicationRequestCharacteristic::getValue).orElse(XM);
    }
}
