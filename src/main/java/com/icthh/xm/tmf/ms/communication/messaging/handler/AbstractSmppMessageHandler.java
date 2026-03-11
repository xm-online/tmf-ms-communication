package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.ParameterNames.OPTIONAL_PARAMETER_PREFIX;
import static java.util.stream.Collectors.toList;

import com.google.common.primitives.Ints;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService.CustomParametersBuilder;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
public abstract class AbstractSmppMessageHandler implements BasicMessageHandler {

    public static final String ERROR_PROCESS_COMMUNICATION_MESSAGE = "Error process communicationMessage ";
    public static final String ERROR_BUSINESS_RULE_VALIDATION = "Error business rule validation";

    protected final KafkaTemplate<String, Object> channelResolver;
    protected final ApplicationProperties applicationProperties;
    protected final BusinessRuleValidator businessRuleValidator;
    protected final CommunicationMessageMapper mapper;

    protected AbstractSmppMessageHandler(KafkaTemplate<String, Object> channelResolver,
        ApplicationProperties applicationProperties,
        BusinessRuleValidator businessRuleValidator,
        CommunicationMessageMapper mapper) {
        this.channelResolver = channelResolver;
        this.applicationProperties = applicationProperties;
        this.businessRuleValidator = businessRuleValidator;
        this.mapper = mapper;
    }

    /**
     * Handles an SMPP message request. Supports the following characteristics:
     * <li>Validity period. A number of seconds a message is valid, uses {@link ParameterNames#VALIDITY_PERIOD}
     * key</li>
     * <li>Protocol id. SMPP protocol id value, uses {@link ParameterNames#PROTOCOL_ID} key.</li>
     * <li>Optional parameters. Uses {@link ParameterNames#OPTIONAL_PARAMETER_PREFIX} key prefix + optional tag value,
     * e.g. <i>OPTIONAL.6005</i></li>
     * <li>Delivery report. Delivery report configuration (byte), uses {@link ParameterNames#DELIVERY_REPORT} key.</li>
     *
     * @param message message request
     * @return {@code message} with {@link ParameterNames#MESSAGE_ID} characteristics that indicates message unique
     * identifier or {@link ParameterNames#ERROR_CODE} in case of error.
     */
    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        List<String> phoneNumbers = message.getReceiver().stream()
            .map(Receiver::getPhoneNumber)
            .collect(toList());

        for (String phoneNumber : phoneNumbers) {
            try {
                validateRules(message);
                String messageId = doSend(message, phoneNumber);
                String queueName = applicationProperties.getMessaging().getSentQueueName();

                sendMessage(success(messageId, message), queueName);
                log.info("Message success sent to {}, messageId {}", queueName, messageId);
                addCharacteristic(message, ParameterNames.MESSAGE_ID, messageId);
            } catch (RuleValidationException e) {
                String responseCode = e.getRuleResponse().getResponseCode();
                log.error("Business rule validation failed, responseCode={}", responseCode);
                failMessage(message, responseCode, ERROR_BUSINESS_RULE_VALIDATION);
                addCharacteristic(message, ParameterNames.ERROR_CODE, responseCode);
            } catch (NegativeResponseException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, "error.system.sending.smpp." + e.getCommandStatus(), e.getMessage());
                addCharacteristic(message, ParameterNames.ERROR_CODE, String.valueOf(e.getCommandStatus()));
            } catch (InvalidResponseException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, ErrorCodes.ERROR_SYSTEM_SENDING_INVALID_RESPONSE, e.getMessage());
                addCharacteristic(message, ParameterNames.ERROR_CODE, ErrorCodes.ERROR_SYSTEM_SENDING_INVALID_RESPONSE);
            } catch (ResponseTimeoutException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, ErrorCodes.ERROR_SYSTEM_SENDING_RESPONSE_TIMEOUT, e.getMessage());
                addCharacteristic(message, ParameterNames.ERROR_CODE, ErrorCodes.ERROR_SYSTEM_SENDING_RESPONSE_TIMEOUT);
            } catch (PDUException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, ErrorCodes.ERROR_SYSTEM_SENDING_PDU, e.getMessage());
                addCharacteristic(message, ParameterNames.ERROR_CODE, ErrorCodes.ERROR_SYSTEM_SENDING_PDU);
            } catch (BusinessException e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, e.getCode(), e.getMessage());
                addCharacteristic(message, ParameterNames.ERROR_CODE, e.getCode());
            } catch (Exception e) {
                log.error(ERROR_PROCESS_COMMUNICATION_MESSAGE, e);
                failMessage(message, ErrorCodes.ERROR_SYSTEM_GENERAL_INTERNAL_SERVER_ERROR, e.toString());
                addCharacteristic(message, ParameterNames.ERROR_CODE,
                    ErrorCodes.ERROR_SYSTEM_GENERAL_INTERNAL_SERVER_ERROR);
            }
        }
        return message;
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        CommunicationMessage message = mapper.messageCreateToMessage(messageCreate);
        return this.handle(message);
    }

    protected abstract String doSend(CommunicationMessage message, String phoneNumber) throws Exception;

    protected byte getDeliveryReport(List<CommunicationRequestCharacteristic> characteristics) {
        return Optional.ofNullable(characteristics).orElse(Collections.emptyList())
            .stream()
            .filter(c -> ParameterNames.DELIVERY_REPORT.equals(c.getName()))
            .findFirst()
            .map(c -> NumberUtils.toByte(c.getValue(), (byte) 0))
            .orElse((byte) 0);
    }

    protected Map<Short, String> buildOptionalParameters(CommunicationMessage message) {
        return Optional.ofNullable(message.getCharacteristic())
            .stream()
            .flatMap(Collection::stream)
            .filter(isOptionalParameter())
            .collect(Collectors.toMap(AbstractSmppMessageHandler::parseParamAsShort,
                CommunicationRequestCharacteristic::getValue));
    }

    protected CustomParametersBuilder buildCustomParameters(List<CommunicationRequestCharacteristic> characteristics) {
        return CustomParametersBuilder.builder()
            .protocolId(getFromCharacteristics(characteristics, ParameterNames.PROTOCOL_ID, Ints::tryParse))
            .validityPeriod(getFromCharacteristics(characteristics, ParameterNames.VALIDITY_PERIOD, Ints::tryParse))
            .sourceTon(findSourceTypeTon(characteristics))
            .destinationTon(findDestinationTypeTon(characteristics))
            .build();
    }

    private void addCharacteristic(CommunicationMessage message, String name, String value) {
        message.addCharacteristicItem(new CommunicationRequestCharacteristic().name(name).value(value));
    }

    private void failMessage(CommunicationMessage message, String code, String errorMessage) {
        String failedQueueName = applicationProperties.getMessaging().getSendFailedQueueName();
        sendMessage(failed(message, code, errorMessage), failedQueueName);
        log.warn("Message about error sent to {}", failedQueueName);
    }

    private void sendMessage(MessageResponse messageResponse, String topic) {
        channelResolver.send(topic, messageResponse);
    }

    private Predicate<CommunicationRequestCharacteristic> isOptionalParameter() {
        return c -> c != null && StringUtils.startsWith(c.getName(), OPTIONAL_PARAMETER_PREFIX);
    }

    private static Short parseParamAsShort(CommunicationRequestCharacteristic ch) {
        return Short.parseShort(ch.getName().substring(OPTIONAL_PARAMETER_PREFIX.length()));
    }

    private String findSourceTypeTon(List<CommunicationRequestCharacteristic> characteristics) {
        return Optional.ofNullable(characteristics).orElse(Collections.emptyList())
            .stream()
            .filter(c -> ParameterNames.SOURCE_TYPE_TON.equals(c.getName()))
            .findFirst()
            .map(CommunicationRequestCharacteristic::getValue)
            .orElse(null);
    }

    private String findDestinationTypeTon(List<CommunicationRequestCharacteristic> characteristics) {
        return Optional.ofNullable(characteristics).orElse(Collections.emptyList())
            .stream()
            .filter(c -> ParameterNames.DESTINATION_TYPE_TON.equals(c.getName()))
            .findFirst()
            .map(CommunicationRequestCharacteristic::getValue)
            .orElse(null);
    }

    private Integer getFromCharacteristics(List<CommunicationRequestCharacteristic> characteristics,
        String key, Function<String, Integer> parseFunction) {
        return Optional.ofNullable(characteristics)
            .stream()
            .flatMap(Collection::stream)
            .filter(ch -> key.equals(ch.getName()))
            .findFirst()
            .map(CommunicationRequestCharacteristic::getValue)
            .map(parseFunction)
            .orElse(null);
    }

    private void validateRules(CommunicationMessage message) {
        RuleResponse ruleResponse = businessRuleValidator.validate(message);
        if (!ruleResponse.isSuccess()) {
            throw new RuleValidationException(ruleResponse);
        }
    }

    static class ErrorCodes {

        public static final String ERROR_SYSTEM_SENDING_INVALID_RESPONSE = "error.system.sending.invalidResponse";
        public static final String ERROR_SYSTEM_SENDING_RESPONSE_TIMEOUT = "error.system.sending.responseTimeout";
        public static final String ERROR_SYSTEM_SENDING_PDU = "error.system.sending.pdu";
        public static final String ERROR_SYSTEM_GENERAL_INTERNAL_SERVER_ERROR = "error.system.general.internalServerError";
    }
}

