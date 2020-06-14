package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service;

import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.common.InfobipMessageStatus;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@Builder
@AllArgsConstructor
public class MessageStatusInfo {
    String messageId;
    @Nullable
    CommunicationMessage communicationMessage;
    InfobipMessageStatus infobipStatus;
}
