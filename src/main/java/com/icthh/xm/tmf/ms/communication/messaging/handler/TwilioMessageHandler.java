package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.tmf.ms.communication.service.TwilioService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper.INSTANCE;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioMessageHandler implements BasicMessageHandler {

    private final TwilioService twilioService;
    private final TenantContextHolder tenantContextHolder;

    @Override
    public void handle(CommunicationMessage message) {
        CommunicationMessageCreate communicationMessageCreate = INSTANCE.messageToMessageCreate(message);
        this.handle(communicationMessageCreate);
    }

    @Override
    public void handle(CommunicationMessageCreate messageCreate) {
        CommunicationMessage send = twilioService.send(tenantContextHolder.getTenantKey(), messageCreate);

    }

}
