package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.response;

import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.common.InfobipMessageStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfobipSendResponseMessage {
    String messageId;
    InfobipSendResponseMessageTo to;
    InfobipMessageStatus status;
}
