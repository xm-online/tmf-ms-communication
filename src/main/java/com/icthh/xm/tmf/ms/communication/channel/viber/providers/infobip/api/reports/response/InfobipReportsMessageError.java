package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.reports.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfobipReportsMessageError {
    Integer groupId;
    String groupName;
    Integer id;
    String name;
    String description;
    Boolean permanent;
}