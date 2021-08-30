package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@LepService(group = "service.message")
@Slf4j
public class EmailMessageHandler implements BasicMessageHandler {

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
        List<String> emails = messageCreate.getReceiver().stream()
            .map(Receiver::getEmail)
            .collect(Collectors.toList());

        for (String email : emails) {
            mailService.sendEmailWithContent(
                TenantContextUtils.getRequiredTenantKey(tenantContextHolder),
                messageCreate.getContent(),
                messageCreate.getSubject(),
                email,
                messageCreate.getSender().getId()
            );
        }
        return mapper.messageCreateToMessage(messageCreate);
    }

    @Override
    public MessageType getType() {
        return MessageType.Email;
    }
}
