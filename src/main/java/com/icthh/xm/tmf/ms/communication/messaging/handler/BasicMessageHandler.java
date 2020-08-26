package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;

public interface BasicMessageHandler {

    void handle(CommunicationMessage message);

    void handle(CommunicationMessageCreate messageCreate);

    MessageType getType();
}
