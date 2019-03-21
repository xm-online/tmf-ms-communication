package com.icthh.xm.tmf.ms.communication.messaging;

import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.DataSm;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;

public interface MessageReceiverListenerAdapter extends MessageReceiverListener {

    @Override
    default void onAcceptAlertNotification(AlertNotification alertNotification) {}

    @Override
    default DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
        return null;
    }
}
