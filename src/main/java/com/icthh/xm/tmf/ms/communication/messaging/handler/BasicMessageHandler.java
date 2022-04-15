package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;

/**
 * Describes a service that handles(processes) messages based on it's type
 * (see {@link com.icthh.xm.tmf.ms.communication.domain.MessageType}).
 * Thus each implementation is responsible for a particular message channel:
 * SMS, Telegram, MobileApp/Firebase, Email etc. <p/>
 * Supports different types of Communication message input that is a matter
 * of form and processing is basically the same.
 */
public interface BasicMessageHandler {

    CommunicationMessage handle(CommunicationMessage message);

    CommunicationMessage handle(CommunicationMessageCreate messageCreate);

    MessageType getType();

}
