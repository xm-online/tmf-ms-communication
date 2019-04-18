package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class MessagingHandler {

    private static final String NOT_BUSINESS_TIME_CODE = "error.business.sending.notBusinessTime";

    private final KafkaTemplate<String, Object> channelResolver;
    private final SmppService smppService;
    private final ApplicationProperties applicationProperties;
    private final BusinessRuleValidator businessRuleValidator;

    private void sendMessage(MessageResponse messageResponse, String topic) {
        channelResolver.send(topic, messageResponse);
    }

    public void receiveMessage(CommunicationMessage message) {
        Messaging messaging = applicationProperties.getMessaging();
        List<String> phoneNumbers = new ArrayList<>(from(message).getPhoneNumbers());
        for (String phoneNumber : phoneNumbers) {
            try {
                List<String> validationResult = businessRuleValidator.validate(message);
                if (validationResult.contains(NOT_BUSINESS_TIME_CODE)) {
                    log.error(NOT_BUSINESS_TIME_CODE);
                    String failedQueueName = messaging.getSendFailedQueueName();
                    sendMessage(failed(message, new BusinessException(NOT_BUSINESS_TIME_CODE)), failedQueueName);
                } else {
                    String messageId = smppService.send(phoneNumber, message.getContent(), message.getSender().getId());
                    String queueName = messaging.getSentQueueName();
                    sendMessage(success(messageId, message), queueName);
                    log.info("Message success sent to {}", queueName);
                }
            } catch (Exception e) {
                log.error("Error process message ", e);
                String failedQueueName = messaging.getSendFailedQueueName();
                sendMessage(failed(message, e), failedQueueName);
                log.warn("Message about error sent to {}", failedQueueName);
            }
        }
    }

}
