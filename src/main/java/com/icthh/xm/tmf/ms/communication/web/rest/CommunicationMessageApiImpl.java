package com.icthh.xm.tmf.ms.communication.web.rest;

import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.utils.ApiMapper;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    private final SmppService smppService;

    CommunicationMessageApiImpl(SmppService smppService) {
        this.smppService = smppService;
    }

    @Timed
    public ResponseEntity<CommunicationMessage> createsANewCommunicationMessageAndSendIt(
        CommunicationMessageCreate message) {
        ApiMapper.CommunicationMessageWrapper wrapper = from(message);
        smppService.sendMultipleMessages(wrapper.getPhoneNumbers(), message.getContent(),
            message.getSender().getId(), wrapper.getDeliveryReport());
        return ResponseEntity.ok().build();
    }

}
