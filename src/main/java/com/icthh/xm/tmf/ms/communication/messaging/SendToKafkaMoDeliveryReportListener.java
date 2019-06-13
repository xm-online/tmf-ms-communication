package com.icthh.xm.tmf.ms.communication.messaging;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;

@Slf4j
public class SendToKafkaMoDeliveryReportListener extends AbstractDeliveryReportListener {

    private final MessagingAdapter messagingAdapter;

    public SendToKafkaMoDeliveryReportListener(MessagingAdapter messagingAdapter, ExecutorService executorService) {
        super(executorService);
        this.messagingAdapter = messagingAdapter;
    }

    @Override
    @SneakyThrows
    public void processDeliveryReport(DeliverSm deliverSm) {
        final StopWatch stopWatch = StopWatch.createStarted();

        if (getState(deliverSm) == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(NON_NULL);
            objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                                              .withFieldVisibility(ANY)
                                              .withGetterVisibility(NONE)
                                              .withSetterVisibility(NONE)
                                              .withCreatorVisibility(NONE));
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            MapType mapType = typeFactory.constructMapType(Map.class, String.class, Object.class);
            Map<String, Object> valueMap = objectMapper.convertValue(deliverSm, mapType);
            valueMap.put("messageText", getMessageBody(deliverSm));
            String message = objectMapper.writeValueAsString(valueMap);
            log.info("Send MO deliver message {}", message);
            messagingAdapter.moDeliveryReport(message);
        }

        log.info("Delivery report processed, time = {}", stopWatch.getTime());
    }

    private String getMessageBody(DeliverSm deliverSm) {
        byte[] bytes = Optional.ofNullable(deliverSm.getOptionalParameter(OptionalParameter.Message_payload.class))
                               .map(OctetString::getValue)
                               .filter(ArrayUtils::isNotEmpty)
                               .orElse(deliverSm.getShortMessage());

        return Optional.ofNullable(bytes)
                       .filter(ArrayUtils::isNotEmpty)
                       .map(it -> decodeContent(it, deliverSm.getDataCoding()))
                       .orElse("");
    }

    @SneakyThrows
    private String decodeContent(byte[] content, byte dataConding) {
        return new String(content, dataConding == 8 ? UTF_16 : UTF_8);
    }

}
