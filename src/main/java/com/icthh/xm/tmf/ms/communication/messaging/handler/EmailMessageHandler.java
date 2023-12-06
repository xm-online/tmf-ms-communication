package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.tmf.ms.communication.domain.EmailReceiver;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;

import java.util.List;

import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

import static java.util.stream.Collectors.toList;


@Service
@RequiredArgsConstructor
@LepService(group = "service.message")
@Slf4j
public class EmailMessageHandler implements BasicMessageHandler {

    public static final String EMAIL_BCC = "BCC";
    private final MailService mailService;
    private final CommunicationMessageMapper mapper;
    private final TenantContextHolder tenantContextHolder;

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageResolver.class)
    public CommunicationMessage handle(CommunicationMessage message) {
        return null;
    }

    @Override
    @LogicExtensionPoint(value = "Send", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage handle(CommunicationMessageCreate messageCreate) {
        log.debug("Handling message {}", messageCreate);
        messageCreate.getReceiver().forEach(receiver ->
            mailService.sendEmailWithContent(
                TenantContextUtils.getRequiredTenantKey(tenantContextHolder),
                messageCreate.getContent(),
                messageCreate.getSubject(),
                new EmailReceiver(receiver.getEmail(), extractBcc(receiver.getCharacteristic())),
                messageCreate.getSender().getId()
            ));

        return mapper.messageCreateToMessage(messageCreate);
    }

    @Override
    public MessageType getType() {
        return MessageType.Email;
    }

    public static EmailReceiver toEmailReceiver(Receiver receiver) {
        return new EmailReceiver(receiver.getEmail(), extractBcc(receiver.getCharacteristic()));
    }

    public static List<String> extractBcc(List<CommunicationRequestCharacteristic> characteristic) {
        characteristic = characteristic == null ? List.of() : characteristic;
        return characteristic.stream()
            .filter(c -> EMAIL_BCC.equals(c.getName()))
            .map(CommunicationRequestCharacteristic::getValue)
            .collect(toList());
    }
}
