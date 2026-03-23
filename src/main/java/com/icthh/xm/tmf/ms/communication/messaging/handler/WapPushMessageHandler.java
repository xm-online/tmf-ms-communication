package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.service.SmppService.CustomParametersBuilder;
import com.icthh.xm.tmf.ms.communication.service.WapPushSegmentationService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@LepService(group = "service.wap.push.message")
@Service
@Slf4j
public class WapPushMessageHandler extends AbstractSmppMessageHandler {

    private final SmppService smppService;
    private final WapPushSegmentationService segmentationService;

    public WapPushMessageHandler(KafkaTemplate<String, Object> channelResolver,
        SmppService smppService,
        WapPushSegmentationService segmentationService,
        ApplicationProperties applicationProperties,
        BusinessRuleValidator businessRuleValidator,
        CommunicationMessageMapper mapper) {
        super(channelResolver, applicationProperties, businessRuleValidator, mapper);
        this.smppService = smppService;
        this.segmentationService = segmentationService;
    }

    @Override
    public MessageType getType() {
        return MessageType.WapPush;
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
        String hexPayload = resolveHexPayload(message);
        List<byte[]> segments = segmentationService.buildWapPushSegmentDetails(hexPayload);
        log.info("Sending WAP Push to {}: totalSegments={}", phoneNumber, segments.size());

        byte deliveryReport = getDeliveryReport(message.getCharacteristic());
        String senderId = Optional.ofNullable(message.getSender()).map(Sender::getId).orElse(null);
        CustomParametersBuilder customParameters = buildCustomParameters(message.getCharacteristic());
        Map<Short, String> optionalParameters = buildOptionalParameters(message);

        List<String> messageIds = new ArrayList<>(segments.size());

        for (int i = 0; i < segments.size(); i++) {
            String messageId = smppService.sendBinary(phoneNumber, senderId, deliveryReport, segments.get(i),
                customParameters, optionalParameters);
            messageIds.add(messageId);
            log.info("WAP Push segment {}/{} submitted: messageId={}", i + 1, segments.size(), messageId);
        }
        return String.join(",", messageIds);
    }

    private String resolveHexPayload(CommunicationMessage message) {
        String content = message.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                "WAP Push message content must contain the hex-encoded OMA/WSP payload");
        }
        return content;
    }
}
