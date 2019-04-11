package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.DeliveryReport.deliveryReport;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.DISTRIBUTION_ID;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.FAILED;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.SUCCESS;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.jsmpp.bean.MessageState.DELIVERED;
import static org.jsmpp.bean.MessageState.UNDELIVERABLE;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_STATE;
import static org.jsmpp.bean.OptionalParameter.Tag.RECEIPTED_MESSAGE_ID;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@RunWith(MockitoJUnitRunner.class)
public class MessagingTest {

    public static final String SUCCESS_SENT = "success_sent";
    public static final String FAIL_SEND = "fail_send";
    public static final String TO_SEND = "to_send";
    public static final String SUCCESS_DELIVERY = "delivery_success";
    public static final String FAILED_DELIVERY = "delivery_failed";

    @InjectMocks
    private MessagingHandler messagingHandler;
    @Mock
    private BinderAwareChannelResolver channelResolver;
    @Mock
    private SmppService smppService;
    @Spy
    private ApplicationProperties applicationProperties = createApplicationProperties();

    private SendToKafkaDeliveryReportListener sendToKafkaDeliveryReportListener;

    @Before
    public void setUp() {
        ExecutorService executorService = ImmediateEventExecutor.INSTANCE;
        MessagingAdapter messagingAdapter = new MessagingAdapter(channelResolver, applicationProperties);
        sendToKafkaDeliveryReportListener = new SendToKafkaDeliveryReportListener(messagingAdapter, executorService);
    }

    private ApplicationProperties createApplicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        Messaging messaging = new Messaging();
        applicationProperties.setMessaging(messaging);
        messaging.setToSendQueueName(TO_SEND);
        messaging.setSentQueueName(SUCCESS_SENT);
        messaging.setSendFailedQueueName(FAIL_SEND);
        messaging.setDeliveryFailedQueueName(FAILED_DELIVERY);
        messaging.setDeliveredQueueName(SUCCESS_DELIVERY);
        return applicationProperties;
    }

    @Test
    public void receiveMessageSuccessTest() {
        MessageChannel messageChannel = mock(MessageChannel.class);
        when(channelResolver.resolveDestination(SUCCESS_SENT)).thenReturn(messageChannel);
        messagingHandler.receiveMessage(message());
        verify(channelResolver).resolveDestination(SUCCESS_SENT);

        MessageResponse messageResponse = new MessageResponse(SUCCESS, message());

        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(argumentCaptor.capture());
        MessageResponse payload = (MessageResponse) argumentCaptor.getValue().getPayload();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        assertThat(payload, equalTo(messageResponse));
    }

    @Test
    public void receiveMessageSuccessWithDistributionIdTest() {
        MessageChannel messageChannel = mock(MessageChannel.class);
        when(channelResolver.resolveDestination(SUCCESS_SENT)).thenReturn(messageChannel);
        CommunicationMessage message = message();
        addDistributionId(message);
        messagingHandler.receiveMessage(message);
        verify(channelResolver).resolveDestination(SUCCESS_SENT);

        CommunicationMessage assertMessage = message();
        addDistributionId(assertMessage);
        MessageResponse messageResponse = new MessageResponse(SUCCESS, assertMessage);

        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(argumentCaptor.capture());
        MessageResponse payload = (MessageResponse) argumentCaptor.getValue().getPayload();
        assertThat(payload.getDistributionId(), equalTo("TEST_D_ID-SMS-ID"));
        assertThat(payload, equalTo(messageResponse));
    }

    private void addDistributionId(CommunicationMessage message) {
        message.setCharacteristic(new ArrayList<>());
        CommunicationRequestCharacteristic disctributionId = new CommunicationRequestCharacteristic().name(DISTRIBUTION_ID)
                                                                                                     .value("TEST_D_ID");
        message.getCharacteristic().add(disctributionId);
    }

    @Test
    public void receiveMessageFailTest() {
        MessageChannel messageChannel = mock(MessageChannel.class);
        when(channelResolver.resolveDestination(FAIL_SEND)).thenReturn(messageChannel);

        when(smppService.send("PH", "TestContext", "TestSender"))
            .thenThrow(new BusinessException("TestMessage"));

        messagingHandler.receiveMessage(message());

        verify(channelResolver).resolveDestination(FAIL_SEND);

        MessageResponse messageResponse = new MessageResponse(FAILED, message());
        messageResponse.setErrorCode("BusinessException");
        messageResponse.setErrorMessage("TestMessage");

        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getPayload(), equalTo(messageResponse));
    }


    @Test
    public void messageUndeliveredTest() {
        MessageChannel messageChannel = mock(MessageChannel.class);
        when(channelResolver.resolveDestination(FAILED_DELIVERY)).thenReturn(messageChannel);
        DeliverSm deliverSm = new DeliverSm();
        OctetString messageId = new OctetString(RECEIPTED_MESSAGE_ID, "messagenumber");
        OptionalParameter.Byte messageState = new OptionalParameter.Byte(MESSAGE_STATE, UNDELIVERABLE.value());
        deliverSm.setOptionalParameters(messageId, messageState);

        sendToKafkaDeliveryReportListener.onAcceptDeliverSm(deliverSm);

        verify(channelResolver).resolveDestination(FAILED_DELIVERY);
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getPayload(), equalTo(deliveryReport("messagenumber", "UNDELIVERABLE")));
    }

    @Test
    public void messageDeliveredTest() {
        MessageChannel messageChannel = mock(MessageChannel.class);
        when(channelResolver.resolveDestination(SUCCESS_DELIVERY)).thenReturn(messageChannel);
        DeliverSm deliverSm = new DeliverSm();
        OctetString messageId = new OctetString(RECEIPTED_MESSAGE_ID, "messagenumber");
        OptionalParameter.Byte messageState = new OptionalParameter.Byte(MESSAGE_STATE, DELIVERED.value());
        deliverSm.setOptionalParameters(messageId, messageState);

        sendToKafkaDeliveryReportListener.onAcceptDeliverSm(deliverSm);

        verify(channelResolver).resolveDestination(SUCCESS_DELIVERY);
        ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getPayload(), equalTo(deliveryReport("messagenumber", "DELIVERED")));
    }

    private CommunicationMessage message() {
        CommunicationMessage message = new CommunicationMessage();
        Receiver receiver = new Receiver();
        receiver.setPhoneNumber("PH");
        receiver.setId("ID");
        Sender sender = new Sender();
        sender.setId("TestSender");
        message.setSender(sender);
        message.setContent("TestContext");
        message.setReceiver(singletonList(receiver));
        message.setType("SMS");
        return message;
    }
}
