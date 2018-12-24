package com.icthh.xm.tmf.ms.communication.web.rest;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    private final SmppService smppService;

    CommunicationMessageApiImpl(SmppService smppService) {
        this.smppService = smppService;
    }

    public ResponseEntity<CommunicationMessage> createsANewCommunicationMessageAndSendIt(
        CommunicationMessageCreate communicationMessageCreate) {

        List<String> phoneNumbers = communicationMessageCreate
            .getReceiver()
            .stream()
            .map(Receiver::getPhoneNumber)
            .collect(Collectors.toList());

        String senderId = ofNullable(communicationMessageCreate)
            .map(CommunicationMessageCreate::getSender)
            .map(Sender::getId).orElse(EMPTY);
        smppService.sendMultipleMessages(phoneNumbers, communicationMessageCreate.getContent(), senderId);

        return ResponseEntity.ok().build();
    }

}
