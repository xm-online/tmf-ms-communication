package com.icthh.xm.tmf.ms.communication.channel;

import java.util.List;

public interface ChannelHandler<T> {

    void onRefresh(String tenantKey, List<T> channels);
}
