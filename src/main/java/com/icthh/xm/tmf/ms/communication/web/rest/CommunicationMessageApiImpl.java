package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
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


    public ResponseEntity<CommunicationMessage> createsANewCommunicationMessageAndSendIt(CommunicationMessageCreate communicationMessageCreate) {
        List<String> phoneNumbers = communicationMessageCreate.getReceiver().stream().map(Receiver::getPhoneNumber).collect(Collectors.toList());
        smppService.sendMultipleMessages(phoneNumbers, communicationMessageCreate.getContent());
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<List<CommunicationMessage>> listCommunicationMessage(String fields,
                                                                        Integer offset,
                                                                        Integer limit) {
        return ResponseEntity.ok(null);
    }

}
