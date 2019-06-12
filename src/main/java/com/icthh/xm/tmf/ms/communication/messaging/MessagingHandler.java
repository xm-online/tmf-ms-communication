package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class MessagingHandler {

    public static final String ERROR_PROCESS_COMMUNICATION_MESSAGE = "Error process communicationMessage ";
    private final KafkaTemplate<String, Object> channelResolver;
    private final SmppService smppService;
    private final ApplicationProperties applicationProperties;

    private void sendMessage(MessageResponse messageResponse, String topic) {
        channelResolver.send(topic, messageResponse);
    }

    public void receiveMessage(CommunicationMessage message) {
        Messaging messaging = applicationProperties.getMessaging();
        List<String> phoneNumbers = new ArrayList<>(from(message).getPhoneNumbers());
        for (String phoneNumber : phoneNumbers) {
            try {
                String messageId = smppService.send(phoneNumber, message.getContent(), message.getSender().getId());
                String queueName = messaging.getSentQueueName();
                sendMessage(success(messageId, message), queueName);
                log.info("Message success sended to {}", queueName);
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
        log.warn("Message about error sended to {}", failedQueueName);
    }

}
