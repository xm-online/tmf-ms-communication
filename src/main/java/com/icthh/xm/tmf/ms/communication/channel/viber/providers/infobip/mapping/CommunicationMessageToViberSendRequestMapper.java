package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.mapping;

import com.google.common.collect.Lists;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequest;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequestDestination;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequestDestinationTo;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequestViber;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config.InfobipViberConfig;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CommunicationMessageToViberSendRequestMapper {

    public static final String VIBER_BUTTON_TEXT_CHARACTERISTIC = "VIBER_BUTTON_TEXT";
    public static final String VIBER_BUTTON_URL_CHARACTERISTIC = "VIBER_BUTTON_URL";
    public static final String VIBER_IMAGE_URL_CHARACTERISTIC = "VIBER_IMAGE_URL";
    public static final String VIBER_PROMOTIONAL_CHARACTERISTIC = "VIBER_PROMOTIONAL";
    public static final String VIBER_VALIDITY_PERIOD_CHARACTERISTIC = "VIBER_VALIDITY_PERIOD";

    public InfobipSendRequest toSendRequest(InfobipViberConfig infobipViberConfig, CommunicationMessage message) {
        Map<String, String> characteristicsMap = message.getCharacteristic()
            .stream()
            .collect(Collectors.toMap(
                CommunicationRequestCharacteristic::getName,
                CommunicationRequestCharacteristic::getValue)
            );

        return InfobipSendRequest
            .builder()
            .destinations(Lists.newArrayList(InfobipSendRequestDestination
                    .builder()
                    .messageId(message.getId())
                    .to(InfobipSendRequestDestinationTo
                        .builder()
                        .phoneNumber(message.getReceiver().get(0).getPhoneNumber())
                        .build())
                    .build()
                )
            )
            .viber(InfobipSendRequestViber
                .builder()
                .buttonText(characteristicsMap.get(VIBER_BUTTON_TEXT_CHARACTERISTIC))
                .buttonUrl(characteristicsMap.get(VIBER_BUTTON_URL_CHARACTERISTIC))
                .imageUrl(characteristicsMap.get(VIBER_IMAGE_URL_CHARACTERISTIC))
                .isPromotional(isPromotional(characteristicsMap))
                .validityPeriod(getValidityPeriod(characteristicsMap))
                .text(message.getContent())
                .build()
            )
            .scenarioKey(infobipViberConfig.getScenarioKey())
            .build();
    }

    private Boolean isPromotional(Map<String, String> characteristics) {
        String promotionalCharacteristic = characteristics.get(VIBER_PROMOTIONAL_CHARACTERISTIC);
        return promotionalCharacteristic != null ? Boolean.valueOf(promotionalCharacteristic) : null;
    }

    private Integer getValidityPeriod(Map<String, String> characteristics) {
        String validityPeriodCharacteristic = characteristics.get(VIBER_VALIDITY_PERIOD_CHARACTERISTIC);
        return validityPeriodCharacteristic != null ? Integer.valueOf(validityPeriodCharacteristic) : null;
    }

}
