package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static java.util.stream.Collectors.toList;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmppMessagingHandler implements BasicMessageHandler {

    public static final String ERROR_PROCESS_COMMUNICATION_MESSAGE = "Error process communicationMessage ";
    public static final String ERROR_BUSINESS_RULE_VALIDATION = "Error business rule validation";
    public static final String DELIVERY_REPORT = "DELIVERY.REPORT";
    public static final String OPTIONAL_PARAMETER_PREFIX = "OPTIONAL.";
    public static final String MESSAGE_ID = "MESSAGE.ID";
    public static final String ERROR_CODE = "ERROR.CODE";
    public static final String VALIDITY_PERIOD = "VALIDITY.PERIOD";

    private final KafkaTemplate<String, Object> channelResolver;
    private final SmppService smppService;
    private final ApplicationProperties applicationProperties;
    private final BusinessRuleValidator businessRuleValidator;
    private final CommunicationMessageMapper mapper;

    @Override
    public CommunicationMessage handle(CommunicationMessage message) {
            Messaging messaging = applicationProperties.getMessaging();
        List<String> phoneNumbers = message.getReceiver().stream().map(Receiver::getPhoneNumber).collect(toList());
        for (String phoneNumber : phoneNumbers) {
            try {
                String messageId = sendSmppMessage(message, messaging, phoneNumber);
                message.getCharacteristic().add(buildCharacteristic(MESSAGE_ID, messageId));
            } catch (NegativeResponseException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.sending.smpp." + e.getCommandStatus(), e.getMessage());
                message.getCharacteristic().add(buildCharacteristic(ERROR_CODE, String.valueOf(e.getCommandStatus())));
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
        return message;
    }

    private CommunicationRequestCharacteristic buildCharacteristic(String key, String value) {
        CommunicationRequestCharacteristic characteristic = new CommunicationRequestCharacteristic();
        characteristic.setName(key);
        characteristic.setValue(value);
        return characteristic;
    }

    @Override
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        CommunicationMessage communicationMessage = mapper.messageCreateToMessage(messageCreate);
        this.handle(communicationMessage);
        return communicationMessage;
    }

    private String sendSmppMessage(CommunicationMessage message, Messaging messaging,
                                   String phoneNumber) throws Exception {
        RuleResponse ruleResponse = businessRuleValidator.validate(message);
        if (!ruleResponse.isSuccess()) {
            failMessage(message, ruleResponse.getResponseCode(), ERROR_BUSINESS_RULE_VALIDATION);
            return phoneNumber;
        }
        String messageId = smppService.send(phoneNumber, message.getContent(), message.getSender().getId(),
            getDeliveryReport(message.getCharacteristic()), buildOptionalParameters(message),
            getValidityPeriod(message.getCharacteristic()));
        String queueName = messaging.getSentQueueName();
        sendMessage(success(messageId, message), queueName);
        log.info("Message success sent to {}, messageId {}", queueName, messageId);
        return messageId;
    }

    private String getValidityPeriod(List<CommunicationRequestCharacteristic> characteristic) {
        return Optional.ofNullable(characteristic)
            .flatMap(c -> c.stream()
                .filter(ch -> VALIDITY_PERIOD.equals(ch.getName()))
                .findFirst()
                .map(CommunicationRequestCharacteristic::getValue))
            .orElse(null);
    }

    private Map<Short, String> buildOptionalParameters(CommunicationMessage message) {
        return Optional.ofNullable(message.getCharacteristic())
            .map(characteristics -> characteristics.stream()
                .filter(Objects::nonNull)
                .filter(c -> Objects.nonNull(c.getName()))
                .filter(ch -> ch.getName().startsWith(OPTIONAL_PARAMETER_PREFIX))
                .collect(Collectors.toMap(
                    ch -> Short.parseShort(ch.getName().substring(OPTIONAL_PARAMETER_PREFIX.length())),
                    CommunicationRequestCharacteristic::getValue)))
            .orElse(Collections.emptyMap());
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

}
