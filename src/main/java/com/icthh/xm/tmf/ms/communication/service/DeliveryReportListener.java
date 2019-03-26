package com.icthh.xm.tmf.ms.communication.service;

import org.jsmpp.bean.DeliverSm;

public interface DeliveryReportListener {
    void onAcceptDeliverSm(DeliverSm deliverSm);
}
