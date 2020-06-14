package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper.INSTANCE;
import static java.util.stream.Collectors.toList;

//@Service
@RequiredArgsConstructor
@Slf4j
public class SmppMessagingHandler implements BasicMessageHandler {

    public static final String ERROR_PROCESS_COMMUNICATION_MESSAGE = "Error process communicationMessage ";
    public static final String ERROR_BUSINESS_RULE_VALIDATION = "Error business rule validation";
    public static final String DELIVERY_REPORT = "DELIVERY.REPORT";
    public static final String VALIDITY_PERIOD = "VALIDITY.PERIOD";
    private final KafkaTemplate<String, Object> channelResolver;
    private final SmppService smppService;
    private final ApplicationProperties applicationProperties;
    private final BusinessRuleValidator businessRuleValidator;

    @Override
    public void handle(CommunicationMessage message) {
        Messaging messaging = applicationProperties.getMessaging();
        List<String> phoneNumbers = message.getReceiver().stream().map(Receiver::getPhoneNumber).collect(toList());
        for (String phoneNumber : phoneNumbers) {
            try {
                sendSmppMessage(message, messaging, phoneNumber);
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

    @Override
    public void handle(CommunicationMessageCreate messageCreate) {

        CommunicationMessage communicationMessage = INSTANCE.messageCreateToMessage(messageCreate);
        this.handle(communicationMessage);
    }

    private void sendSmppMessage(CommunicationMessage message, Messaging messaging,
                                 String phoneNumber) throws Exception {
        RuleResponse ruleResponse = businessRuleValidator.validate(message);
        if (!ruleResponse.isSuccess()) {
            failMessage(message, ruleResponse.getResponseCode(), ERROR_BUSINESS_RULE_VALIDATION);
            return;
        }
        String messageId = smppService.send(phoneNumber, message.getContent(), message.getSender().getId(),
            getDeliveryReport(message.getCharacteristic()), getValidityPeriod(message.getCharacteristic()));
        String queueName = messaging.getSentQueueName();
        sendMessage(success(messageId, message), queueName);
        log.info("Message success sent to {}", queueName);
    }

    private void failMessage(CommunicationMessage communicationMessage, String code, String message) {
        Messaging messaging = applicationProperties.getMessaging();
        String failedQueueName = messaging.getSendFailedQueueName();
        sendMessage(failed(communicationMessage, code, message), failedQueueName);
        log.warn("Message about error sent to {}", failedQueueName);
    }

    private void sendMessage(MessageResponse messageResponse, String topic) {
        channelResolver.send(topic, messageResponse);
    }

    byte getDeliveryReport(List<CommunicationRequestCharacteristic> characteristics) {
        return Optional.ofNullable(characteristics).orElse(Collections.emptyList())
            .stream()
            .filter(c -> DELIVERY_REPORT.equals(c.getName()))
            .findFirst()
            .map(c -> NumberUtils.toByte(c.getValue(), (byte) 0))
            .orElse((byte) 0);
    }

    private String getValidityPeriod(List<CommunicationRequestCharacteristic> characteristics) {
        return Optional.ofNullable(characteristics).orElse(Collections.emptyList())
            .stream()
            .filter(c -> VALIDITY_PERIOD.equals(c.getName()))
            .map(CommunicationRequestCharacteristic::getValue)
            .findFirst()
            .orElse("");
    }
}
