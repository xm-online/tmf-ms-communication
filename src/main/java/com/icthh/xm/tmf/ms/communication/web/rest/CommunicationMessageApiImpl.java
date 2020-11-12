package com.icthh.xm.tmf.ms.communication.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    private final MessageHandlerService messageHandlerService;

    @Timed
    @PreAuthorize("hasPermission({'messageCreate': #messageCreate}, 'COMMUNICATION.MESSAGE.SEND')")
    @PrivilegeDescription("Privilege to create and send communication messages")
    @Override
    public ResponseEntity<CommunicationMessage> createsANewCommunicationMessageAndSendIt(
        CommunicationMessageCreate messageCreate) {
        CommunicationMessage message = messageHandlerService.getHandler(messageCreate.getType()).handle(messageCreate);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'COMMUNICATION.MESSAGE.RETRIEVE')")
    @PrivilegeDescription("Privilege to retrieve communication messages")
    @Override
    public ResponseEntity<List<CommunicationMessage>> retrieveCommunicationMessage(String id) {
        List<CommunicationMessage> messages = messageHandlerService.retrieveCommunicationMessage(id);
        return ResponseEntity.ok(messages);
    }
}
