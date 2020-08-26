package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service.ViberService;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper.INSTANCE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViberMessageHandler implements BasicMessageHandler {

    private final ViberService viberService;

    @Override
    public void handle(CommunicationMessage message) {
        viberService.send(message);
    }

    @Override
    public void handle(CommunicationMessageCreate messageCreate) {
        CommunicationMessage communicationMessage = INSTANCE.messageCreateToMessage(messageCreate);
        this.handle(communicationMessage);
    }

    @Override
    public MessageType getType() {
        return MessageType.Viber;
    }
}
