package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.channel.mobileapp.FirebaseApplicationConfigurationProvider;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;

import com.icthh.xm.tmf.ms.communication.messaging.handler.logic.FirebaseMessageHelper;
import com.icthh.xm.tmf.ms.communication.service.firebase.FirebaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@LepService(group = "service.message")
@ConditionalOnBean(FirebaseApplicationConfigurationProvider.class)
public class MobileAppMessageHandler extends FirebaseMessageHandler {

    public MobileAppMessageHandler(FirebaseService firebaseService,
                                   CommunicationMessageMapper mapper,
                                   FirebaseMessageHelper firebaseMessageHelper) {
        super(firebaseService, mapper, firebaseMessageHelper);
    }

    @Override
    public MessageType getType() {
        return MessageType.MobileApp;
    }
}
