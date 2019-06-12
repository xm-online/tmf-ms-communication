package com.icthh.xm.tmf.ms.communication.messaging;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.service.DeliveryReportListener;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.DeliverSm;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDeliveryReportListener implements DeliveryReportListener {
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

    public abstract void processDeliveryReport(DeliverSm deliverSm);

}
