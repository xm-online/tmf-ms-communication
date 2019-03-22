package com.icthh.xm.tmf.ms.communication.messaging;

import static java.util.Arrays.asList;
import static org.jsmpp.bean.MessageState.DELIVERED;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.DeliveryReport;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
@RequiredArgsConstructor
public class MessagingAdapter {

    private final BinderAwareChannelResolver channelResolver;
    private final ApplicationProperties applicationProperties;

    public void deliveryReport(DeliveryReport deliveryReport) {
        ApplicationProperties.Messaging messaging = applicationProperties.getMessaging();
        String topic = deliveryReport.getDeliveryStatus().equals(DELIVERED.name()) ? messaging.getDeliveredQueueName() :
                       messaging.getDeliveryFailedQueueName();
        channelResolver.resolveDestination(topic).send(MessageBuilder.withPayload(deliveryReport).build());
    }

}
