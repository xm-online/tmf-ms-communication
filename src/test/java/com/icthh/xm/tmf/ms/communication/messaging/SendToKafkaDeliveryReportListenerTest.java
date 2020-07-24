package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.DeliveryReport.deliveryReport;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.jsmpp.bean.MessageState.DELIVERED;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import com.icthh.xm.tmf.ms.communication.domain.DeliveryReport;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageState;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.OptionalParameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SendToKafkaDeliveryReportListenerTest {

    @Mock
    private MessagingAdapter messagingAdapter;

    @InjectMocks
    private SendToKafkaDeliveryReportListener deliveryReportListener;

    @Test
    public void processDeliveryReport_shouldTakeMessageIdFromDeliveryReceipt() {
        DeliverSm deliverSm = new DeliverSm();
        deliverSm.setEsmClass(MessageType.SMSC_DEL_RECEIPT.value());
        deliverSm.setShortMessage(
            "id:2 sub:001 dlvrd:001 submit date:0908312310 done date:0908312311 stat:DELIVRD err:xxx Text:Hello world!"
                .getBytes());

        deliveryReportListener.processDeliveryReport(deliverSm);

        ArgumentCaptor<DeliveryReport> argumentCaptor = ArgumentCaptor.forClass(DeliveryReport.class);
        verify(messagingAdapter).deliveryReport(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), equalTo(deliveryReport("2", "DELIVERED")));
    }

    @Test
    public void processDeliveryReport_shouldTakeMessageIdFromOptionalParametersIfShortMessageIsNull() {
        String expectedMessageId = "7";
        MessageState expectedState = DELIVERED;

        DeliverSm deliverSm = new DeliverSm();
        OptionalParameter messageIdParameter = new OptionalParameter.Receipted_message_id(expectedMessageId);
        OptionalParameter stateParameter = new OptionalParameter.Message_state(expectedState.value());
        deliverSm.setOptionalParameters(messageIdParameter, stateParameter);

        deliveryReportListener.processDeliveryReport(deliverSm);

        ArgumentCaptor<DeliveryReport> argumentCaptor = ArgumentCaptor.forClass(DeliveryReport.class);
        verify(messagingAdapter).deliveryReport(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), equalTo(deliveryReport(expectedMessageId, expectedState.name())));
    }
}
