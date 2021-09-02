package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.service.TwilioService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioMessageHandler implements BasicMessageHandler {

    private final TwilioService twilioService;
    private final TenantContextHolder tenantContextHolder;
    private final CommunicationMessageMapper mapper;

    @Override
    public CommunicationMessage handle(CommunicationMessage message) {
        CommunicationMessageCreate communicationMessageCreate = mapper.messageToMessageCreate(message);
        return handle(communicationMessageCreate);
    }

    @Override
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        CommunicationMessage send = twilioService.send(tenantContextHolder.getTenantKey(), messageCreate);
        log.info("message status={} href={}", send.getStatus(), send.getHref());
        return send;
    }

    @Override
    public MessageType getType() {
        return MessageType.Twilio;
    }
}
