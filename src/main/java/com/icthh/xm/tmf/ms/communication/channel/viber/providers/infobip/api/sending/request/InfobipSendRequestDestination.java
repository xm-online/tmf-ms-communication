package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfobipSendRequestDestination {
    InfobipSendRequestDestinationTo to;
    String messageId;
}
