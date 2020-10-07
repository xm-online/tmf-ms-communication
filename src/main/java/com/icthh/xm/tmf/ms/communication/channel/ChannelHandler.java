package com.icthh.xm.tmf.ms.communication.channel;

import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;

public interface ChannelHandler {

    /**
     * Refreshes the current configuration with the new one.
     *
     * @param tenantKey tenant key(name)
     * @param spec configuration specification
     */
    void onRefresh(String tenantKey, CommunicationSpec spec);

}
