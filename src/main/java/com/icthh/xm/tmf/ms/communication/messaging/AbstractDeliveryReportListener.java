package com.icthh.xm.tmf.ms.communication.messaging;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.service.DeliveryReportListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.MessageState;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.util.DeliveryReceiptState;
import org.jsmpp.util.InvalidDeliveryReceiptException;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static org.jsmpp.bean.MessageState.ACCEPTED;
import static org.jsmpp.bean.MessageState.DELIVERED;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_STATE;
import static org.jsmpp.bean.OptionalParameter.Tag.RECEIPTED_MESSAGE_ID;
import static org.jsmpp.util.DeliveryReceiptState.*;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDeliveryReportListener implements DeliveryReportListener {
    private final ExecutorService executorService;

    @Override
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

    protected String getMessageId(DeliveryReceipt deliveryReceipt){
        String messageId = deliveryReceipt.getId();
        try {
            return ofNullable(messageId)
                .map(id-> new BigInteger(id, 10))
                .map(id-> id.toString(16))
                .orElse(null);
        } catch (Exception e) {
            log.error("Cannot convert delivered message id to big integer, id: {}", messageId);
            return null;
        }
    }

    protected MessageState getState(DeliveryReceipt deliveryReceipt) {
        DeliveryReceiptState finalStatus = deliveryReceipt.getFinalStatus();
        return ofNullable(finalStatus).map(messageStatusMap::get).orElse(null);
    }

    protected String getMessageId(DeliverSm deliverSm) {
        return getTagValue(deliverSm, RECEIPTED_MESSAGE_ID, op -> {
            byte[] value = ((OptionalParameter.OctetString) op).getValue();
            String tagValue = new String(value, Charset.defaultCharset());
            return StringUtils.trim(tagValue);
        });
    }

    protected MessageState getState(DeliverSm deliverSm) {
        return getTagValue(deliverSm, MESSAGE_STATE, op -> {
            return MessageState.valueOf(((OptionalParameter.Byte) op).getValue());
        });
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

    private static final Map<DeliveryReceiptState, MessageState> messageStatusMap = new EnumMap(DeliveryReceiptState.class) {{
        put(ACCEPTD, ACCEPTED);
        put(DELETED, MessageState.DELETED);
        put(DELIVRD, DELIVERED);
        put(ENROUTE, MessageState.ENROUTE);
        put(EXPIRED, MessageState.EXPIRED);
        put(UNKNOWN, MessageState.UNKNOWN);
        put(UNDELIV, MessageState.UNDELIVERABLE);
        put(REJECTD, MessageState.REJECTED);
    }};

}
