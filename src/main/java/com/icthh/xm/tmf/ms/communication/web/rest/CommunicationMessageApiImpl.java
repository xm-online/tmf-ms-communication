package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    private final SmppService smppService;

    CommunicationMessageApiImpl(SmppService smppService) {
        this.smppService = smppService;
    }

    @Override
    public ResponseEntity<CommunicationMessage> sendsACommunicationMessage(String id) {
        smppService.send(id, "Hello");
        return null;
    }

    @Override
    public ResponseEntity<List<CommunicationMessage>> listCommunicationMessage(String fields,
                                                                        Integer offset,
                                                                        Integer limit) {
        return ResponseEntity.ok(null);
    }

}
