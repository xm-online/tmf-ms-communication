package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.reports.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InfobipReportsResponse {
    List<InfobipReportsMessageResult> results;
}
