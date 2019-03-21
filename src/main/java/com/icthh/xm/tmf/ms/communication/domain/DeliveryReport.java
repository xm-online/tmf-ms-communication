package com.icthh.xm.tmf.ms.communication.domain;

import lombok.Data;

@Data
public class DeliveryReport {

    private String deliveryStatus;
    private String messageId;

    public static DeliveryReport deliveryReport(String messageId, String deliveryStatus) {
        DeliveryReport deliveryReport = new DeliveryReport();
        deliveryReport.setDeliveryStatus(deliveryStatus);
        deliveryReport.setMessageId(messageId);
        return deliveryReport;
    }

}
