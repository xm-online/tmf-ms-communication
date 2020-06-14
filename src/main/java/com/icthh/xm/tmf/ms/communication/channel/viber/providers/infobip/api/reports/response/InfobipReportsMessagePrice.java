package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.reports.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfobipReportsMessagePrice {
    Double pricePerMessage;
    String currency;
}
