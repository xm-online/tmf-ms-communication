package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.DeliveryReport.deliveryReport;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_STATE;
import static org.jsmpp.bean.OptionalParameter.Tag.RECEIPTED_MESSAGE_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageState;
import org.jsmpp.bean.OptionalParameter;

@Slf4j
public class SendToKafkaOmDeliveryReportListener extends AbstractDeliveryReportListener {

    private final MessagingAdapter messagingAdapter;

    public SendToKafkaOmDeliveryReportListener(MessagingAdapter messagingAdapter, ExecutorService executorService) {
        super(executorService);
        this.messagingAdapter = messagingAdapter;
    }

    @Override
    @SneakyThrows
    public void processDeliveryReport(DeliverSm deliverSm) {
        final StopWatch stopWatch = StopWatch.createStarted();
        ObjectMapper objectMapper = new ObjectMapper();
        messagingAdapter.omDeliveryReport(objectMapper.writeValueAsString(deliverSm));
        log.info("Delivery report processed, time = {}", stopWatch.getTime());
    }
}
