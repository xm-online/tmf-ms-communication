package com.icthh.xm.tmf.ms.communication.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    private final MessageHandlerService messageHandlerService;

    @Timed
    public ResponseEntity<CommunicationMessage> createsANewCommunicationMessageAndSendIt(
        CommunicationMessageCreate messageCreate) {
        CommunicationMessage message = messageHandlerService.getHandler(messageCreate.getType()).handle(messageCreate);
        return ResponseEntity.ok(message);
    }
}
