package com.icthh.xm.tmf.ms.communication.messaging.handler;

import static java.util.stream.Collectors.toMap;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@LepService(group = "service.message")
@Slf4j
public class TemplatedEmailMessageHandler implements BasicMessageHandler {

    private static final String TEMPLATE_NAME = "templateName";
    private static final String LANGUAGE = "language";

    private final MailService mailService;
    private final CommunicationMessageMapper mapper;

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        return null;
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        log.debug("Handling message {}", messageCreate);
        List<String> emails = messageCreate.getReceiver().stream()
            .map(Receiver::getEmail)
            .collect(Collectors.toList());

        Map<String, Object> objectModel = toObjectModel(messageCreate.getCharacteristic());
        String language = String.valueOf(objectModel.get(LANGUAGE));
        String templateName = String.valueOf(objectModel.get(TEMPLATE_NAME));
        Locale locale = new Locale(language);
        String sender = messageCreate.getSender().getId();

        for (String email : emails) {
            mailService.sendEmailFromTemplate(
                locale,
                templateName,
                null,
                email,
                sender,
                objectModel
            );
        }
        return mapper.messageCreateToMessage(messageCreate);
    }

    @Override
    public MessageType getType() {
        return MessageType.TemplatedEmail;
    }

    private Map<String, Object> toObjectModel(List<CommunicationRequestCharacteristic> characteristics) {
        return characteristics.stream()
            .collect(toMap(CommunicationRequestCharacteristic::getName, CommunicationRequestCharacteristic::getValue));
    }
}
