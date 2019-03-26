package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;
import static java.util.Arrays.asList;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
@RequiredArgsConstructor
public class MessagingHandler {

    private final BinderAwareChannelResolver channelResolver;
    private final SmppService smppService;
    private final ApplicationProperties applicationProperties;

    private void sendMessage(MessageResponse messageResponse, String topic) {
        channelResolver.resolveDestination(topic).send(MessageBuilder.withPayload(messageResponse).build());
    }

    public void receiveMessage(CommunicationMessageCreate message) {
        Messaging messaging = applicationProperties.getMessaging();
        List<String> phoneNumbers = new ArrayList<>();
        phoneNumbers.addAll(from(message).getPhoneNumbers());
        for (String phoneNumber : phoneNumbers) {
            try {
                String messageId = smppService.send(phoneNumber, message.getContent(), message.getSender().getId());
                String queueName = messaging.getSentQueueName();
                sendMessage(success(messageId, message), queueName);
                log.info("Message success sended to {}", queueName);
            } catch (Exception e) {
                log.error("Error process message ", e);
                String failedQueueName = messaging.getSendFailedQueueName();
                sendMessage(failed(message, e), failedQueueName);
                log.warn("Message about erro sended to {}", failedQueueName);
            }
        }
    }

}
