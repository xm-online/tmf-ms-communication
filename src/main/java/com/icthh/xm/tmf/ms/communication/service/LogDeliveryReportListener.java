package com.icthh.xm.tmf.ms.communication.service;

import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.DeliverSm;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogDeliveryReportListener implements DeliveryReportListener {

    @Override
    public void onAcceptDeliverSm(DeliverSm deliverSm) {
        log.info("onAcceptDeliverSm | deliverSm: {}", deliverSm);
    }

}
