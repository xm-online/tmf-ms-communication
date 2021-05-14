package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.ExtendedCommunicationMessage;
import org.springframework.beans.BeanUtils;

public class ExtendedCommunicationMessageFactory {
    public static ExtendedCommunicationMessage newMessage() {
        ExtendedCommunicationMessage message = new ExtendedCommunicationMessage();
        message.atType("ExtendedCommunicationMessage");
        message.atBaseType("CommunicationMessage");
        message.atSchemaLocation("https://github.com/xm-online/tmf-ms-communication/blob/master/src/main/resources/swagger/api-extension.yml");
        return message;
    }

    public static ExtendedCommunicationMessage newMessage(CommunicationMessage source) {
        ExtendedCommunicationMessage message = newMessage();
        BeanUtils.copyProperties(source, message);
        return message;
    }
}
