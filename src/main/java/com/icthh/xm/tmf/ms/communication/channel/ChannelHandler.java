package com.icthh.xm.tmf.ms.communication.channel;

import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;

public interface ChannelHandler {

    void onRefresh(String tenantKey, CommunicationSpec spec);

}
