package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    @Override
    public ResponseEntity<List<CommunicationMessage>> listCommunicationMessage(String fields,
                                                                        Integer offset,
                                                                        Integer limit) {
        return ResponseEntity.ok(null);
    }

}
