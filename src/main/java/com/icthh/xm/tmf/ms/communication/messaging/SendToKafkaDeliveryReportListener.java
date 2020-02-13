package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.DeliveryReport.deliveryReport;
import static java.util.Optional.ofNullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.MessageState;
import org.jsmpp.util.DeliveryReceiptState;

@Slf4j
public class SendToKafkaDeliveryReportListener extends AbstractDeliveryReportListener {

    private final MessagingAdapter messagingAdapter;
    private final boolean convertToHexDeliveredId;

    public SendToKafkaDeliveryReportListener(MessagingAdapter messagingAdapter,
                                             ExecutorService executorService,
                                             boolean convertToHexDeliveredId
    ) {
        super(executorService);
        this.messagingAdapter = messagingAdapter;
        this.convertToHexDeliveredId = convertToHexDeliveredId;
    }

    @Override
    public void processDeliveryReport(DeliverSm deliverSm) {
        final StopWatch stopWatch = StopWatch.createStarted();

        DeliveryReceipt shortMessage = getShortMessage(deliverSm);
        String messageId = ofNullable(getMessageId(shortMessage, convertToHexDeliveredId)).orElseGet(() -> getMessageId(deliverSm));
        MessageState state = ofNullable(getState(shortMessage)).orElseGet(() -> getState(deliverSm));

        log.info("Delivery report is received with smsc id = {}, state = {}, short message: {}", messageId, state, shortMessage);

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
