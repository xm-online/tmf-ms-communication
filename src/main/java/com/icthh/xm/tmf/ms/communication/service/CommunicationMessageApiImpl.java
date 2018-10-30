package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    @Override
    public ResponseEntity<List<CommunicationMessage>> listCommunicationMessage(String fields,
                                                                        Integer offset,
                                                                        Integer limit) {
        throw new BusinessException("error.code", "text message");
    }

}
