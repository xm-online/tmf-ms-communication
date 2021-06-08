package com.icthh.xm.tmf.ms.communication.messaging;

import static java.util.Optional.ofNullable;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_STATE;
import static org.jsmpp.bean.OptionalParameter.Tag.RECEIPTED_MESSAGE_ID;
import static org.jsmpp.util.DeliveryReceiptState.ACCEPTD;
import static org.jsmpp.util.DeliveryReceiptState.DELETED;
import static org.jsmpp.util.DeliveryReceiptState.DELIVRD;
import static org.jsmpp.util.DeliveryReceiptState.ENROUTE;
import static org.jsmpp.util.DeliveryReceiptState.EXPIRED;
import static org.jsmpp.util.DeliveryReceiptState.REJECTD;
import static org.jsmpp.util.DeliveryReceiptState.UNDELIV;
import static org.jsmpp.util.DeliveryReceiptState.UNKNOWN;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.service.DeliveryReportListener;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.MessageState;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.util.DeliveryReceiptState;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.springframework.cloud.sleuth.annotation.NewSpan;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDeliveryReportListener implements DeliveryReportListener {
    public static final int DECIMAL_SYSTEM = 10;
    public static final int HEX_SYSTEM = 16;
    private final ExecutorService executorService;

    @Override
    @NewSpan
    public void onAcceptDeliverSm(DeliverSm deliverSm) {
        String rid = MdcUtils.getRid();
        executorService.submit(() -> {
            try {
                MdcUtils.putRid(rid);
                processDeliveryReport(deliverSm);
            } catch (Throwable t) {
                log.error("Error process delivery report ", t);
            } finally {
                MdcUtils.removeRid();
            }
        });
    }

    protected DeliveryReceipt getShortMessage(DeliverSm deliverSm) {
        try {
            return deliverSm.getShortMessageAsDeliveryReceipt();
        } catch (InvalidDeliveryReceiptException e) {
            log.error("Cannot get short message, error: {}", e.getMessage());
        }
        return null;
    }

    protected String getMessageId(DeliveryReceipt deliveryReceipt) {
        try {
            return ofNullable(deliveryReceipt)
                .map(DeliveryReceipt::getId)
                .map(messageId -> new BigInteger(messageId, DECIMAL_SYSTEM))
                .map(id -> id.toString(HEX_SYSTEM))
                .orElse(null);
        } catch (Exception e) {
            log.error("Cannot convert delivered message id to big integer: {}", deliveryReceipt);
            return null;
        }
    }

    protected MessageState getState(DeliveryReceipt deliveryReceipt) {
        return ofNullable(deliveryReceipt)
            .map(DeliveryReceipt::getFinalStatus)
            .map(messageStatusMap::get)
            .orElse(null);
    }

    protected String getMessageId(DeliverSm deliverSm) {
        return getTagValue(deliverSm, RECEIPTED_MESSAGE_ID, op -> {
            byte[] value = ((OptionalParameter.OctetString) op).getValue();
            String tagValue = new String(value, Charset.defaultCharset());
            return StringUtils.trim(tagValue);
        });
    }

    protected MessageState getState(DeliverSm deliverSm) {
        return getTagValue(deliverSm, MESSAGE_STATE, op ->
            MessageState.valueOf(((OptionalParameter.Byte) op).getValue()));
    }

    protected <T> T getTagValue(DeliverSm deliverSm, OptionalParameter.Tag tag, Function<OptionalParameter, T> converter) {
        if (deliverSm.getOptionalParameters() == null) {
            log.warn("Delivery report is not has option parameters.");
            return null;
        }
        for (OptionalParameter op : deliverSm.getOptionalParameters()) {
            if (op.tag == tag.code()) {
                return converter.apply(op);
            }
        }
        return null;
    }

    public abstract void processDeliveryReport(DeliverSm deliverSm);

    private static final Map<DeliveryReceiptState, MessageState> messageStatusMap = new EnumMap<>(DeliveryReceiptState.class) {{
        put(ACCEPTD, MessageState.ACCEPTED);
        put(DELETED, MessageState.DELETED);
        put(DELIVRD, MessageState.DELIVERED);
        put(ENROUTE, MessageState.ENROUTE);
        put(EXPIRED, MessageState.EXPIRED);
        put(UNKNOWN, MessageState.UNKNOWN);
        put(UNDELIV, MessageState.UNDELIVERABLE);
        put(REJECTD, MessageState.REJECTED);
    }};
}
