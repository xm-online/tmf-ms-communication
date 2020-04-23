package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP;
import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;
import static org.apache.commons.lang3.StringUtils.unwrap;
import static org.springframework.kafka.support.KafkaHeaders.ACKNOWLEDGMENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.utils.ApiMapper;
import com.icthh.xm.tmf.ms.communication.utils.ApiMapper.CommunicationMessageWrapper;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;

import java.util.*;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@Slf4j
@RequiredArgsConstructor
public class MessagingHandler {

    public static final String ERROR_PROCESS_COMMUNICATION_MESSAGE = "Error process communicationMessage ";
    public static final String ERROR_BUSINESS_RULE_VALIDATION = "Error business rule validation";
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> channelResolver;
    private final SmppService smppService;
    private final ApplicationProperties applicationProperties;
    private final BusinessRuleValidator businessRuleValidator;

    private void sendMessage(MessageResponse messageResponse, String topic) {
        channelResolver.send(topic, messageResponse);
    }

    public void handleEvent(Message<?> message) {
        final StopWatch stopWatch = StopWatch.createStarted();

        // ACKNOWLEDGMENT before processing (important, for avoid duplicate sms)
        message.getHeaders().get(ACKNOWLEDGMENT, Acknowledgment.class).acknowledge();

        try {
            String payloadString = (String) message.getPayload();
            log.info("start processing message, base64 body = {}, headers = {}", payloadString, getHeaders(message));
            payloadString = unwrap(payloadString, "\"");
            log.info("start processing message, json body = {}", payloadString);
            CommunicationMessage communicationMessage = mapToCommunicationMessage(payloadString);
            addReceivedByChannelCharacteristic(communicationMessage, message);
            receiveMessage(communicationMessage);
            log.info("stop processing message, time = {}", stopWatch.getTime());
        } catch (Exception e) {
            log.error("Error process event", e);
        }
    }


    public void receiveMessage(CommunicationMessage message) {
        Messaging messaging = applicationProperties.getMessaging();
        CommunicationMessageWrapper wrapper = from(message);
        List<String> phoneNumbers = new ArrayList<>(wrapper.getPhoneNumbers());
        for (String phoneNumber : phoneNumbers) {
            try {
                RuleResponse ruleResponse = businessRuleValidator.validate(message);
                if (!ruleResponse.isSuccess()) {
                    failMessage(message, ruleResponse.getResponseCode(), ERROR_BUSINESS_RULE_VALIDATION);
                    return;
                }
                String messageId = smppService.send(phoneNumber, message.getContent(), message.getSender().getId(),
                    wrapper.getDeliveryReport(), wrapper.getValidityPeriod());
                String queueName = messaging.getSentQueueName();
                sendMessage(success(messageId, message), queueName);
                log.info("Message success sent to {}", queueName);
            } catch (NegativeResponseException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.sending.smpp." + e.getCommandStatus(), e.getMessage());
            } catch (InvalidResponseException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.sending.invalidResponse", e.getMessage());
            } catch (ResponseTimeoutException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.sending.responseTimeout", e.getMessage());
            } catch (PDUException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.sending.pdu", e.getMessage());
            } catch (BusinessException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, e.getCode(), e.getMessage());
            } catch (Exception e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.general.internalServerError", e.toString());
            }
        }
    }

    private void failMessage(CommunicationMessage communicationMessage, String code, String message) {
        Messaging messaging = applicationProperties.getMessaging();
        String failedQueueName = messaging.getSendFailedQueueName();
        sendMessage(failed(communicationMessage, code, message), failedQueueName);
        log.warn("Message about error sent to {}", failedQueueName);
    }


    private Map<String, Object> getHeaders(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        Map<String, Object> headersForLog = new HashMap<>(headers);
        headersForLog.remove(ACKNOWLEDGMENT);
        return headersForLog;
    }

    @SneakyThrows
    private CommunicationMessage mapToCommunicationMessage(String eventBody) {
        return objectMapper.readValue(eventBody, CommunicationMessage.class);
    }

    /**
     * Since Kafka headers are not accessible from the business rules,
     * move Kafka received timestamp to the communication message characteristics
     */
    private void addReceivedByChannelCharacteristic(CommunicationMessage communicationMessage, Message<?> kafkaMessage) {
        Optional.ofNullable(kafkaMessage)
            .map(Message::getHeaders)
            .map(headers -> headers.get(KafkaHeaders.RECEIVED_TIMESTAMP))
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .ifPresent(kafkaReceivedTimestamp ->
                communicationMessage.addCharacteristicItem(
                    new CommunicationRequestCharacteristic()
                        // Rename it to unlink name from source channel
                        .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                        .value(kafkaReceivedTimestamp)
                )
            );
    }

}
