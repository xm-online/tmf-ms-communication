package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;

/**
 * Service for receiving and sending message from different channels
 */
public interface MessageService {

    /**
     * Recive message from channel
     *
     * @param tenantKey tenant key
     * @param message   received message
     * @return message with updated status, flags, etc
     */
    CommunicationMessage receive(String tenantKey, CommunicationMessage message);

    /**
     * Send message to the channel
     *
     * @param tenantKey tenant key
     * @param message   new message
     * @return message with updated id,status, flags, etc
     */
    CommunicationMessage send(String tenantKey, CommunicationMessageCreate message);
}
