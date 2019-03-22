package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.DeliveryReport.deliveryReport;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_STATE;
import static org.jsmpp.bean.OptionalParameter.Tag.RECEIPTED_MESSAGE_ID;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.service.DeliveryReportListener;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageState;
import org.jsmpp.bean.OptionalParameter;

@Slf4j
@RequiredArgsConstructor
public class SendToKafkaDeliveryReportListener implements DeliveryReportListener {

    private final MessagingAdapter messagingAdapter;
    private final ExecutorService executorService;

    @Override
    public void onAcceptDeliverSm(DeliverSm deliverSm) {
        String rid = MdcUtils.getRid();
        executorService.submit(() -> {
            try {
                MdcUtils.putRid(rid);
                processDeliveryReport(deliverSm);
            } finally {
                MdcUtils.removeRid();
            }
        });
    }

    private void processDeliveryReport(DeliverSm deliverSm) {
        final StopWatch stopWatch = StopWatch.createStarted();

        if (deliverSm.getOptionalParameters() == null) {
            log.warn("Delivery report is not has option parameters.");
            return;
        }

        String id = null;
        MessageState state = null;
        for (OptionalParameter op : deliverSm.getOptionalParameters()) {
            // If the message contains information, note on network error
            if (op.tag == RECEIPTED_MESSAGE_ID.code()) {
                byte[] value = ((OptionalParameter.OctetString) op).getValue();
                id = new String(value, Charset.defaultCharset());
                id = StringUtils.trim(id);
            }

            if (op.tag == MESSAGE_STATE.code()) {
                byte value = ((OptionalParameter.Byte) op).getValue();
                state = MessageState.valueOf(value);
            }
        }

        log.info("Delivery report is received with smsc id = {}, state = {}.", id, state);

        if (id == null) {
            log.warn("RECEIPTED_MESSAGE_ID optional parameter not found.");
            return;
        }
        if (state == null) {
            log.warn("MESSAGE_STATE optional parameter not found.");
            return;
        }

        MessageState status = MessageState.valueOf(state.value());
        messagingAdapter.deliveryReport(deliveryReport(id, status.name()));

        log.info("Delivery report processed, time = {}", stopWatch.getTime());
    }
}
