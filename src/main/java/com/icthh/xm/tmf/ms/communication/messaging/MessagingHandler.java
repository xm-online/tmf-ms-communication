package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.failed;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.success;
import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.support.MessageBuilder;

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
                sendMessage(success(messageId, message), messaging.getSentQueueName());
            } catch (Exception e) {
                sendMessage(failed(message, e), messaging.getDeliveryFailedQueueName());
            }
        }
    }

}
