package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfobipMessageStatus {
    Integer groupId;
    String groupName;
    Integer id;
    String name;
    String description;
    String action;
}
