package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service;

import com.google.gson.Gson;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.common.InfobipStatusEnum;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequest;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.response.InfobipSendResponse;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.mapping.CommunicationMessageToViberSendRequestMapper;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.DeliveryReport;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@AllArgsConstructor
@Component
public class ViberService {

    private static final String SEND_PATH = "/omni/1/advanced";
    private static final Gson gson = new Gson();

    private final RestTemplate restTemplate;
    private final ApplicationProperties applicationProperties;
    private final KafkaTemplate<String, Object> channelResolver;

    private final CommunicationMessageToViberSendRequestMapper communicationMessageToViberSendRequestMapper;

    public void send(CommunicationMessage communicationMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.set(AUTHORIZATION, applicationProperties.getInfobip().getToken());

        InfobipSendRequest request = communicationMessageToViberSendRequestMapper.toSendRequest(communicationMessage);
        HttpEntity<String> requestEntity = new HttpEntity<>(gson.toJson(request), headers);

        ResponseEntity<InfobipSendResponse> exchange;
        try {
            exchange = restTemplate.exchange(applicationProperties.getInfobip().getAddress() + SEND_PATH, HttpMethod.POST, requestEntity, InfobipSendResponse.class);
            processMessageStatus(
                Objects.requireNonNull(exchange.getBody()).getMessages().stream()
                    .map(infobipSendResponseMessage -> new MessageStatusInfo(communicationMessage.getId(), communicationMessage, infobipSendResponseMessage.getStatus()))
                    .collect(Collectors.toList()));
        } catch (HttpStatusCodeException e) {
            if (e.getRawStatusCode() != 200) {
                channelResolver.send(
                    applicationProperties.getMessaging().getSendFailedQueueName(),
                    gson.toJson(MessageResponse.failed(communicationMessage, "error.system.sending.viber.gateway.internalError", String.format("Viber provider responded with %s http code", e.getRawStatusCode())))
                );
            }
        }
    }

    public void processMessageStatus(List<MessageStatusInfo> messageStatusInfo) {
        for (MessageStatusInfo statusInfo : messageStatusInfo) {
            if (InfobipStatusEnum.PENDING.getGroupId() == statusInfo.getInfobipStatus().getGroupId()) {
                channelResolver.send(
                    applicationProperties.getMessaging().getSentQueueName(),
                    gson.toJson(MessageResponse.success(statusInfo.getMessageId(), statusInfo.getCommunicationMessage()))
                );
            } else if (InfobipStatusEnum.REJECTED.getGroupId() == statusInfo.getInfobipStatus().getGroupId()) {
                channelResolver.send(
                    applicationProperties.getMessaging().getSendFailedQueueName(),
                    gson.toJson(MessageResponse.failed(statusInfo.getCommunicationMessage(), "error.system.sending.viber.gateway.rejection", statusInfo.getInfobipStatus().getDescription()))
                );
            }
            if (InfobipStatusEnum.DELIVERED.getGroupId() == statusInfo.getInfobipStatus().getGroupId()) {
                channelResolver.send(
                    applicationProperties.getMessaging().getDeliveredQueueName(),
                    DeliveryReport.deliveryReport(statusInfo.getMessageId(), ObmDeliveryReportStatusEnum.DELIVERED.name())
                );
            } else if (InfobipStatusEnum.EXPIRED.getGroupId() == statusInfo.getInfobipStatus().getGroupId()) {
                channelResolver.send(
                    applicationProperties.getMessaging().getDeliveryFailedQueueName(),
                    DeliveryReport.deliveryReport(statusInfo.getMessageId(), ObmDeliveryReportStatusEnum.EXPIRED.name())
                );
            } else if (InfobipStatusEnum.UNDELIVERABLE.getGroupId() == statusInfo.getInfobipStatus().getGroupId()) {
                channelResolver.send(
                    applicationProperties.getMessaging().getDeliveryFailedQueueName(),
                    DeliveryReport.deliveryReport(statusInfo.getMessageId(), ObmDeliveryReportStatusEnum.UNDELIVERABLE.name())
                );
            } else {
                log.error("Unknown group id {}", statusInfo.getInfobipStatus().getGroupId());
            }
        }
    }

}
