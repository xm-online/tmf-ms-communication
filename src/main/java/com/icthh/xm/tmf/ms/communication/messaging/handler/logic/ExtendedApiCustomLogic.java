package com.icthh.xm.tmf.ms.communication.messaging.handler.logic;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.channel.mobileapp.FirebaseApplicationConfigurationProvider;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper;
import com.icthh.xm.tmf.ms.communication.service.firebase.ExtendedCommunicationMessageFactory;
import com.icthh.xm.tmf.ms.communication.web.api.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@LepService(group = "service.message", name = "default")
@Slf4j
@ConditionalOnBean(FirebaseApplicationConfigurationProvider.class)
public class ExtendedApiCustomLogic {
    private final CommunicationMessageMapper mapper;

    /**
     * Merged Firebase responses from splitted by receivers messages in single API response
     * @param responses - responses CommunicationMessage
     * @param messageCreate - original messageCreate, for which responses where received
     * @return CommunicationMessage response merged from list of input responses
     */
    @LogicExtensionPoint(value = "MergeFirebaseResponse", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessage mergeResponse(List<CommunicationMessage> responses, CommunicationMessageCreate messageCreate) {
        int failureCnt = 0, successCnt = 0;
        List<Detail> details = new ArrayList();

        ExtendedCommunicationMessage mergedResponse =
                ExtendedCommunicationMessageFactory.newMessage(mapper.messageCreateToMessage(messageCreate));
        mergedResponse.getReceiver().clear(); //remove all original receivers

        for (CommunicationMessage response : responses) {
            mergedResponse.getReceiver().addAll(response.getReceiver());
            if (response instanceof ExtendedCommunicationMessage) {
                ExtendedCommunicationMessage responseItem = (ExtendedCommunicationMessage) response;
                Result itemResult = responseItem.result();
                failureCnt += itemResult.failureCount();
                successCnt += itemResult.successCount();
                details.addAll(itemResult.details());
            }
        }
        mergedResponse.result(new Result().successCount(successCnt).failureCount(failureCnt).details(details));

        return mergedResponse;
    }

    /**
     * Implement mechanism that will allow to specify custom characteristics applicable for each receiver
     * @param messageCreate .input message
     * @return List of CommunicationMessageCreate messages, grouped by receivers characteristics
     */
    @LogicExtensionPoint(value = "SplitMessagesByCharacteristics", resolver = CustomMessageCreateResolver.class)
    public List<CommunicationMessageCreate> splitMessagesByCharacteristics(final CommunicationMessageCreate messageCreate) {
        List<CommunicationMessageCreate> messageGroups = new ArrayList();

        messageCreate.getReceiver().stream().filter(p -> p.getCharacteristic() == null).forEach(p -> p.setCharacteristic(new ArrayList<>()));
        Map receiverGroups = (Map) messageCreate.getReceiver().stream().collect(Collectors.groupingBy(Receiver::getCharacteristic));

        if (receiverGroups.keySet().size() <= 1) {
            messageGroups.add(messageCreate);
        } else {
            receiverGroups.forEach((a_key, a_value) -> {
                        List<CommunicationRequestCharacteristic> key = (List<CommunicationRequestCharacteristic>) a_key;
                        List<Receiver> value = (List<Receiver>) a_value;
                        CommunicationMessageCreate newMessage = new CommunicationMessageCreate()
                                .receiver(value)
                                .callbackFlag(messageCreate.getCallbackFlag())
                                .content(messageCreate.getContent())
                                .description(messageCreate.getDescription())
                                .logFlag(messageCreate.getLogFlag())
                                .priority(messageCreate.getPriority())
                                .sendTime(messageCreate.getSendTime())
                                .sendTimeComplete(messageCreate.getSendTimeComplete())
                                .status(messageCreate.getStatus())
                                .subject(messageCreate.getSubject())
                                .tryTimes(messageCreate.getTryTimes())
                                .type(messageCreate.getType())
                                .version(messageCreate.getVersion())
                                .attachment(messageCreate.getAttachment())
                                .sender(messageCreate.getSender())
                                .characteristic(new ArrayList(messageCreate.getCharacteristic()))
                                .atType(messageCreate.getAtType())
                                .atSchemaLocation(messageCreate.getAtSchemaLocation())
                                .atBaseType(messageCreate.getAtBaseType());

                        if (key != null) {
                            for (CommunicationRequestCharacteristic characteristicItem : key) {
                                newMessage.getCharacteristic().removeIf(
                                        characteristic -> characteristicItem.getName().equals(characteristic.getName())
                                );
                            }
                            newMessage.getCharacteristic().addAll(key);
                            messageGroups.add(newMessage);
                        }
                    }
            );
        }

        return messageGroups;
    }

    @LogicExtensionPoint(value = "ProcessReceivers", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessageCreate processReceivers(final CommunicationMessageCreate messageCreate) {
        log.info("processReceivers: no custom logic is executed");
        return messageCreate;
    }

    @LogicExtensionPoint(value = "ApplyCharacteristics", resolver = CustomMessageCreateResolver.class)
    public CommunicationMessageCreate applyCharacteristics(final CommunicationMessageCreate messageCreate) {
        log.info("applyCharacteristics: no custom logic is executed");
        return messageCreate;
    }

}
