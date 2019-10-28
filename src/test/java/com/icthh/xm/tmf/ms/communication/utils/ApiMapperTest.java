package com.icthh.xm.tmf.ms.communication.utils;

import com.icthh.xm.tmf.ms.communication.domain.MessageType;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.CommunicationMessageWrapper.DELIVERY_REPORT;
import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;

@RunWith(MockitoJUnitRunner.class)
public class ApiMapperTest {

    @Test
    public void deliveryReportTest() {
        Assert.assertEquals(from(message("1")).getDeliveryReport(), (byte) 1);
        Assert.assertEquals(from(message("0")).getDeliveryReport(), (byte) 0);
        Assert.assertEquals(from(message("9")).getDeliveryReport(), (byte) 9);
        Assert.assertEquals(from(message("homer")).getDeliveryReport(), (byte) 0);
    }

    @Test
    public void emptyCharacteristicsDeliveryReportTest() {
        CommunicationMessage message = new CommunicationMessage();
        message.setType(MessageType.SMS.name());
        Assert.assertEquals(from(message).getDeliveryReport(), (byte) 0);
    }

    @NotNull
    private CommunicationMessage message(String deliveryValue) {
        return new CommunicationMessage() {{
            setType(MessageType.SMS.name());
            setCharacteristic(Lists.newArrayList(new CommunicationRequestCharacteristic() {{
                name(DELIVERY_REPORT);
                value(deliveryValue);
            }}));
        }};
    }
}
