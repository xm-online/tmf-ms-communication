package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.mapping;

import com.google.common.collect.Lists;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequest;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequestDestination;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequestDestinationTo;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request.InfobipSendRequestViber;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.Constants.VIBER_BUTTON_TEXT_CHARACTERISTIC;
import static com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.Constants.VIBER_BUTTON_URL_CHARACTERISTIC;
import static com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.Constants.VIBER_IMAGE_URL_CHARACTERISTIC;
import static com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.Constants.VIBER_INFOBIP_SCENARIO_KEY_CHARACTERISTIC;
import static com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.Constants.VIBER_PROMOTIONAL_CHARACTERISTIC;
import static com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.Constants.VIBER_VALIDITY_PERIOD_CHARACTERISTIC;
import static com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.Utils.collectMessageCharacteristics;

@Component
@AllArgsConstructor
@Slf4j
public class CommunicationMessageToViberSendRequestMapper {

    public InfobipSendRequest toSendRequest(CommunicationMessage message) {
        Map<String, String> characteristicsMap = collectMessageCharacteristics(message);

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
            .scenarioKey(characteristicsMap.get(VIBER_INFOBIP_SCENARIO_KEY_CHARACTERISTIC))
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
