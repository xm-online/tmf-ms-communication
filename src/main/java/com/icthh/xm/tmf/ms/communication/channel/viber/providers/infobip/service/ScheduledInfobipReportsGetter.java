package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service;

import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.reports.response.InfobipReportsResponse;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequest;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@RequiredArgsConstructor
public class ScheduledInfobipReportsGetter {

    public static final String REPORTS_PATH = "/omni/1/reports";
    private final ApplicationProperties applicationProperties;
    private final ViberService viberService;
    private final RestTemplate restTemplate;

    @Scheduled(fixedDelayString = "${application.infobip.statuses.acquiring.delay-millis}")
    public void processStatuses() {
        if (!applicationProperties.getInfobip().getStatuses().getAcquiring().isEnabled()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.set(AUTHORIZATION, applicationProperties.getInfobip().getToken());

        HttpEntity<InfobipSendRequest> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<InfobipReportsResponse> exchange = restTemplate.exchange(applicationProperties.getInfobip().getAddress() + REPORTS_PATH, HttpMethod.GET, requestEntity, InfobipReportsResponse.class);
        viberService.processMessageStatus(Objects.requireNonNull(exchange.getBody()).getResults()
            .stream()
            .map(infobipReportsMessageResult -> new MessageStatusInfo(infobipReportsMessageResult.getMessageId(), null, infobipReportsMessageResult.getStatus()))
            .collect(toList()));
    }
}
