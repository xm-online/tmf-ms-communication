package com.icthh.xm.tmf.ms.communication.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jsmpp.bean.DeliverSm;

@Slf4j
public class SendToKafkaMoDeliveryReportListener extends AbstractDeliveryReportListener {

    private final MessagingAdapter messagingAdapter;

    public SendToKafkaMoDeliveryReportListener(MessagingAdapter messagingAdapter, ExecutorService executorService) {
        super(executorService);
        this.messagingAdapter = messagingAdapter;
    }

    @Override
    @SneakyThrows
    public void processDeliveryReport(DeliverSm deliverSm) {
        final StopWatch stopWatch = StopWatch.createStarted();

        if (getState(deliverSm) == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            messagingAdapter.moDeliveryReport(objectMapper.writeValueAsString(deliverSm));
        }

        log.info("Delivery report processed, time = {}", stopWatch.getTime());

    }
}
