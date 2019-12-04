package com.icthh.xm.tmf.ms.communication.web.rest;

import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.tmf.ms.communication.service.FirebaseService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.service.TelegramService;
import com.icthh.xm.tmf.ms.communication.utils.ApiMapper;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.InternalServerErrorException;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    private final SmppService smppService;

    private final FirebaseService firebaseService;

    private final TelegramService telegramService;

    @Timed
    public ResponseEntity<CommunicationMessage> createsANewCommunicationMessageAndSendIt(
        CommunicationMessageCreate message) {
        ApiMapper.CommunicationMessageWrapper wrapper = from(message);
        switch (wrapper.getType()) {
            case SMS:
                smppService.sendMultipleMessages(wrapper.getPhoneNumbers(), message.getContent(),
                    message.getSender().getId(), wrapper.getDeliveryReport());
                break;
            case MobileApp:
                firebaseService.sendPushNotification(wrapper);
                break;
            case Telegram:
                telegramService.send(null/*TBD*/, message);
                break;
            default:
                throw new InternalServerErrorException(String.format("message type invalid " +
                    "or still not implemented. type: %s", wrapper.getType()));
        }

        return ResponseEntity.ok().build();
    }
}
