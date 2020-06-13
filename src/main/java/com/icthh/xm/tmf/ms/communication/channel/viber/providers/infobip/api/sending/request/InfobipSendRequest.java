package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InfobipSendRequest {
    String messageId;
    String scenarioKey;
    List<InfobipSendRequestDestination> destinations;
    InfobipSendRequestViber viber;
}
