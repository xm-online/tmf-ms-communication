package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.service.mail.MailService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.ExtendedAttachment;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;


@Service
@RequiredArgsConstructor
@LepService(group = "service.message")
@Slf4j
public class TemplatedEmailMessageHandler implements BasicMessageHandler {

    private static final String TEMPLATE_NAME = "templateName";
    private static final String LANGUAGE = "language";

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

        Map<String, Object> objectModel = toObjectModel(messageCreate.getCharacteristic());
        String language = String.valueOf(objectModel.get(LANGUAGE));
        String templateName = String.valueOf(objectModel.get(TEMPLATE_NAME));
        Locale locale = new Locale(language);
        String sender = messageCreate.getSender().getId();
        String subject = messageCreate.getSubject();

        boolean isExtendedAttachment = false;
        Map<String, InputStreamSource> attachments = null;
        if (!messageCreate.getAttachment().isEmpty() && messageCreate.getAttachment().get(0) instanceof ExtendedAttachment) {
            isExtendedAttachment = true;
            attachments = messageCreate.getAttachment().stream()
                .filter(Objects::nonNull)
                .map(it -> (ExtendedAttachment) it)
                .filter(it -> StringUtils.isNotBlank(it.getFileBytes()))
                .collect(Collectors.toMap(ExtendedAttachment::getName,
                    it -> new ByteArrayResource(Base64.decodeBase64(it.getFileBytes()))
                ));
        }

        for (String email : emails) {
            if (isExtendedAttachment) {
                mailService.sendEmailFromTemplateWithAttachments(
                    TenantContextUtils.getRequiredTenantKey(tenantContextHolder.getContext()),
                    locale,
                    templateName,
                    subject,
                    email,
                    objectModel,
                    MdcUtils.generateRid(),
                    sender,
                    attachments
                );
            } else {
                mailService.sendEmailFromTemplate(
                    locale,
                    templateName,
                    subject,
                    email,
                    sender,
                    objectModel
                );
            }
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
