package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.channel.mobileapp.FirebaseApplicationConfigurationProvider;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.messaging.handler.logic.FirebaseMessageHelper;
import com.icthh.xm.tmf.ms.communication.service.firebase.FirebaseService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@LepService(group = "service.message")
@Slf4j
@ConditionalOnBean(FirebaseApplicationConfigurationProvider.class)
public abstract class FirebaseMessageHandler implements BasicMessageHandler {

    protected final FirebaseService firebaseService;
    protected final CommunicationMessageMapper mapper;
    protected final FirebaseMessageHelper firebaseMessageHelper;

    public FirebaseMessageHandler(FirebaseService firebaseService,
                                  CommunicationMessageMapper mapper,
                                  FirebaseMessageHelper firebaseMessageHelper) {
        this.firebaseService = firebaseService;
        this.mapper = mapper;
        this.firebaseMessageHelper = firebaseMessageHelper;
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        log.debug("Handling {}} message {}", getType(), message);
        return firebaseService.sendPushNotification(message);
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        log.debug("Handling {} message {}", getType(), messageCreate);

        CommunicationMessageCreate processedReceivers = firebaseMessageHelper.processReceivers(messageCreate);
        List<CommunicationMessageCreate> processedMessages = firebaseMessageHelper
            .splitMessagesByCharacteristics(processedReceivers);

        List<CommunicationMessage> responses = new ArrayList<>();
        processedMessages.forEach(message -> {
            CommunicationMessageCreate modifiedMessage = firebaseMessageHelper.applyCharacteristics(message);

            modifiedMessage.getCharacteristic().add(
                new CommunicationRequestCharacteristic()
                    .name(ParameterNames.RESULT_TYPE)
                    .value(getResponseStrategy())
            );

            CommunicationMessage firebaseResponse = firebaseService
                .sendPushNotification(mapper.messageCreateToMessage(modifiedMessage));
            responses.add(firebaseResponse);
        });

        return firebaseMessageHelper.mergeResponse(responses, messageCreate);
    }

    public String getResponseStrategy() {
        return "FULL";
    }
}
