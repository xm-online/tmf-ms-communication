package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.DeliveryReport.deliveryReport;

import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageState;

@Slf4j
public class SendToKafkaDeliveryReportListener extends AbstractDeliveryReportListener {

    private final MessagingAdapter messagingAdapter;

    public SendToKafkaDeliveryReportListener(MessagingAdapter messagingAdapter, ExecutorService executorService) {
        super(executorService);
        this.messagingAdapter = messagingAdapter;
    }

    @Override
    public void processDeliveryReport(DeliverSm deliverSm) {
        final StopWatch stopWatch = StopWatch.createStarted();

        String messageId = getMessageId(deliverSm);
        MessageState state = getState(deliverSm);

        log.info("Delivery report is received with smsc id = {}, state = {}.", messageId, state);

        if (messageId == null) {
            log.warn("RECEIPTED_MESSAGE_ID optional parameter not found.");
            return;
        }
        if (state == null) {
            log.warn("MESSAGE_STATE optional parameter not found.");
            return;
        }

        MessageState status = MessageState.valueOf(state.value());
        messagingAdapter.deliveryReport(deliveryReport(messageId, status.name()));

        log.info("Delivery report processed, time = {}", stopWatch.getTime());
    }
}
