package com.icthh.xm.tmf.ms.communication.messaging;

import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_STATE;
import static org.jsmpp.bean.OptionalParameter.Tag.RECEIPTED_MESSAGE_ID;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.service.DeliveryReportListener;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageState;
import org.jsmpp.bean.OptionalParameter;

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

    protected String getMessageId(DeliverSm deliverSm) {
        return getTagValue(deliverSm, RECEIPTED_MESSAGE_ID, op -> {
            byte[] value = ((OptionalParameter.OctetString) op).getValue();
            String tagValue = new String(value, Charset.defaultCharset());
            return StringUtils.trim(tagValue);
        });
    }

    protected MessageState getState(DeliverSm deliverSm) {
        return getTagValue(deliverSm, RECEIPTED_MESSAGE_ID, op -> {
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

}
