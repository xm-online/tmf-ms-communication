package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.channel.mobileapp.FirebaseApplicationConfigurationProvider;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.messaging.handler.logic.ExtendedApiCustomLogic;
import com.icthh.xm.tmf.ms.communication.service.firebase.FirebaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@LepService(group = "service.message")
@Slf4j
@ConditionalOnBean(FirebaseApplicationConfigurationProvider.class)
public class WebPushMessageHandler extends FirebaseMessageHandler  {

    public WebPushMessageHandler(FirebaseService firebaseService, CommunicationMessageMapper mapper, ExtendedApiCustomLogic extendedApiCustomLogic) {
        super(firebaseService, mapper, extendedApiCustomLogic);
    }

    @Override
    public MessageType getType() {
        return MessageType.WebPush;
    }
}
