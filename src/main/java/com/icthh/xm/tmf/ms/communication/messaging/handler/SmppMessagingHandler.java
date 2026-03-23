package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@LepService(group = "service.message")
@Service
@Slf4j
public class SmppMessagingHandler extends AbstractSmppMessageHandler {

    private final SmppService smppService;

    public SmppMessagingHandler(KafkaTemplate<String, Object> channelResolver,
        SmppService smppService,
        ApplicationProperties applicationProperties,
        BusinessRuleValidator businessRuleValidator,
        CommunicationMessageMapper mapper) {
        super(channelResolver, applicationProperties, businessRuleValidator, mapper);
        this.smppService = smppService;
    }

    @Override
    public MessageType getType() {
        return MessageType.SMS;
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        return super.handle(message);
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        return super.handle(messageCreate);
    }

    @Override
    protected String doSend(CommunicationMessage message, String phoneNumber) throws Exception {
        List<CommunicationRequestCharacteristic> characteristics = message.getCharacteristic();
        return smppService.send(phoneNumber, message.getContent(), message.getSender().getId(),
            getDeliveryReport(characteristics), buildOptionalParameters(message),
            buildCustomParameters(characteristics));
    }

}
