package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.reports.response;

import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.common.InfobipMessageStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class InfobipReportsMessageResult {
    String bulkId;
    String messageId;
    String channel;
    String to;
    Date sentAt;
    Date seenAt;
    Date doneAt;
    Integer messageCount;
    String mccMnc;
    InfobipReportsMessagePrice price;
    InfobipMessageStatus status;
    InfobipReportsMessageError error;
}
