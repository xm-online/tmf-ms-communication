package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static java.util.stream.Collectors.toList;

import com.google.common.primitives.Ints;
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
import java.util.function.Function;
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
    public static final String PROTOCOL_ID = "PROTOCOL.ID";

    private final KafkaTemplate<String, Object> channelResolver;
    private final SmppService smppService;
    private final ApplicationProperties applicationProperties;
    private final BusinessRuleValidator businessRuleValidator;
    private final CommunicationMessageMapper mapper;

    /**
     * Handles an SMPP message request.
     * Supports the following characteristics:
     * <li>Validity period. A number of seconds a message is valid, uses {@link #VALIDITY_PERIOD} key</li>
     * <li>Protocol id. SMPP protocol id value, uses {@link #PROTOCOL_ID} key.</li>
     * <li>Optional parameters. Uses {@link #OPTIONAL_PARAMETER_PREFIX} key prefix + optional tag value,
     * e.g. <i>OPTIONAL.6005</i></li>
     * <li>Delivery report. Delivery report configuration (byte), uses {@link #DELIVERY_REPORT} key.</li>
     *
     * @param message message request
     * @return {@code message} with {@link #MESSAGE_ID} characteristics that
     * indicates message unique identifier or {@link #ERROR_CODE} in case of error.
     */
    @Override
    public CommunicationMessage handle(CommunicationMessage message) {
            Messaging messaging = applicationProperties.getMessaging();
        List<String> phoneNumbers = message.getReceiver().stream().map(Receiver::getPhoneNumber).collect(toList());
        for (String phoneNumber : phoneNumbers) {
            try {
                String messageId = sendSmppMessage(message, messaging, phoneNumber);
                addCharacteristic(message, MESSAGE_ID, messageId);
            } catch (RuleValidationException e) {
                String responseCode = e.getRuleResponse().getResponseCode();
                log.error(ERROR_BUSINESS_RULE_VALIDATION + ", responseCode: " + responseCode);
                failMessage(message, responseCode, ERROR_BUSINESS_RULE_VALIDATION);
                addCharacteristic(message, ERROR_CODE, String.valueOf(responseCode));
            } catch (NegativeResponseException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.sending.smpp." + e.getCommandStatus(), e.getMessage());
                addCharacteristic(message, ERROR_CODE, String.valueOf(e.getCommandStatus()));
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

    private boolean addCharacteristic(CommunicationMessage message, String errorCode, String s) {
        return message.getCharacteristic().add(buildCharacteristic(errorCode, s));
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
            throw new RuleValidationException(ruleResponse);
        }

        String messageId = smppService.send(phoneNumber, message.getContent(), message.getSender().getId(),
            getDeliveryReport(message.getCharacteristic()), buildOptionalParameters(message),
            getFromCharacteristics(message.getCharacteristic(), VALIDITY_PERIOD, Ints::tryParse),
            getFromCharacteristics(message.getCharacteristic(), PROTOCOL_ID, Ints::tryParse)
        );
        String queueName = messaging.getSentQueueName();
        sendMessage(success(messageId, message), queueName);
        log.info("Message success sent to {}, messageId {}", queueName, messageId);
        return messageId;
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

    private Integer getFromCharacteristics(List<CommunicationRequestCharacteristic> characteristic,
                                           String key, Function<String, Integer> parseFunction) {
        return Optional.ofNullable(characteristic)
            .flatMap(c -> c.stream()
                .filter(ch -> key.equals(ch.getName()))
                .findFirst()
                .map(CommunicationRequestCharacteristic::getValue))
            .map(parseFunction)
            .orElse(null);
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
