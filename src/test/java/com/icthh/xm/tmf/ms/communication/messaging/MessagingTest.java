package com.icthh.xm.tmf.ms.communication.messaging;

import static com.icthh.xm.tmf.ms.communication.domain.DeliveryReport.deliveryReport;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.DISTRIBUTION_ID;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.FAILED;
import static com.icthh.xm.tmf.ms.communication.domain.MessageResponse.Status.SUCCESS;
import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.CommunicationMessageWrapper.DELIVERY_REPORT;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.jsmpp.bean.MessageState.DELIVERED;
import static org.jsmpp.bean.MessageState.UNDELIVERABLE;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_STATE;
import static org.jsmpp.bean.OptionalParameter.Tag.RECEIPTED_MESSAGE_ID;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.BusinessRule;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Messaging;
import com.icthh.xm.tmf.ms.communication.domain.DeliveryReport;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import io.netty.util.concurrent.ImmediateEventExecutor;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.jsmpp.PDUException;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.extra.NegativeResponseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.MessageChannel;
import org.testcontainers.shaded.com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class MessagingTest {

    public static final String SUCCESS_SENT = "success_sent";
    public static final String FAIL_SEND = "fail_send";
    public static final String TO_SEND = "to_send";
    public static final String SUCCESS_DELIVERY = "delivery_success";
    public static final String FAILED_DELIVERY = "delivery_failed";
    public static final String MO_QUEUE = "MO-QUEUE";

    @InjectMocks
    private MessagingHandler messagingHandler;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private SmppService smppService;
    @Mock
    private BusinessRuleValidator businessRuleValidator;
    @Spy
    private ApplicationProperties applicationProperties = createApplicationProperties();

    private SendToKafkaDeliveryReportListener sendToKafkaDeliveryReportListener;
    private SendToKafkaMoDeliveryReportListener sendToKafkaMoDeliveryReportListener;

    @Before
    public void setUp() {
        ExecutorService executorService = ImmediateEventExecutor.INSTANCE;
        MessagingAdapter messagingAdapter = new MessagingAdapter(kafkaTemplate, applicationProperties);
        sendToKafkaDeliveryReportListener = new SendToKafkaDeliveryReportListener(messagingAdapter, executorService, true);
        sendToKafkaMoDeliveryReportListener = new SendToKafkaMoDeliveryReportListener(messagingAdapter, executorService);
        RuleResponse response = new RuleResponse();
        response.setSuccess(true);
        when(businessRuleValidator.validate(any())).thenReturn(response);
    }

    public static ApplicationProperties createApplicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        Messaging messaging = new Messaging();
        applicationProperties.setMessaging(messaging);
        messaging.setToSendQueueName(TO_SEND);
        messaging.setSentQueueName(SUCCESS_SENT);
        messaging.setSendFailedQueueName(FAIL_SEND);
        messaging.setDeliveryFailedQueueName(FAILED_DELIVERY);
        messaging.setDeliveredQueueName(SUCCESS_DELIVERY);
        messaging.setDeliveredMoQueueName(MO_QUEUE);
        BusinessRule businessRule = new BusinessRule();
        businessRule.setEnableBusinessTimeRule(false);
        applicationProperties.setBusinessRule(businessRule);
        return applicationProperties;
    }

    @Test
    public void receiveMessageSuccessTest() {
        messagingHandler.receiveMessage(message());

        MessageResponse messageResponse = new MessageResponse(SUCCESS, message());

        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), argumentCaptor.capture());
        MessageResponse payload = argumentCaptor.getValue();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        assertThat(payload, equalTo(messageResponse));
    }

    @Test
    public void receiveMessageSuccessWithDistributionIdTest() {
        CommunicationMessage message = message();
        addDistributionId(message);
        messagingHandler.receiveMessage(message);

        CommunicationMessage assertMessage = message();
        addDistributionId(assertMessage);
        MessageResponse messageResponse = new MessageResponse(SUCCESS, assertMessage);

        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(kafkaTemplate).send(eq(SUCCESS_SENT), argumentCaptor.capture());
        MessageResponse payload = (MessageResponse) argumentCaptor.getValue();
        assertThat(payload.getId(), equalTo("TEST_D_ID-SMS-ID"));
        assertThat(payload.getDistributionId(), equalTo("TEST_D_ID"));
        assertThat(payload, equalTo(messageResponse));
    }

    private void addDistributionId(CommunicationMessage message) {
        message.setCharacteristic(new ArrayList<>());
        CommunicationRequestCharacteristic distributionId = new CommunicationRequestCharacteristic().name(DISTRIBUTION_ID)
                                                                                                     .value("TEST_D_ID");
        message.getCharacteristic().add(distributionId);
    }

    @Test
    @SneakyThrows
    public void receiveMessageFailTest() {
        failMessage(new RuntimeException("TestMessage"),
            "error.system.general.internalServerError", "java.lang.RuntimeException: TestMessage");
        reset(smppService, kafkaTemplate);
        failMessage(new BusinessException("TestCode", "TestMessage"), "TestCode", "TestMessage");
        reset(smppService, kafkaTemplate);
        failMessage(new PDUException("TestMessage"), "error.system.sending.pdu", "TestMessage");
        reset(smppService, kafkaTemplate);
        failMessage(new NegativeResponseException(20), "error.system.sending.smpp.20", "Negative response 00000014 (Message Queue Full) found");

    }

    @SneakyThrows
    private void failMessage(Exception e, String errorCode, String testMessage) {
        when(smppService.send("PH", "TestContext", "TestSender", (byte) 1)).thenThrow(e);

        messagingHandler.receiveMessage(message());

        MessageResponse messageResponse = new MessageResponse(FAILED, message());
        messageResponse.setErrorCode(errorCode);
        messageResponse.setErrorMessage(testMessage);

        ArgumentCaptor<MessageResponse> argumentCaptor = ArgumentCaptor.forClass(MessageResponse.class);

        verify(kafkaTemplate).send(eq(FAIL_SEND), argumentCaptor.capture());
        MessageResponse payload = argumentCaptor.getValue();
        payload.setId(null);
        payload.setDistributionId(null);
        messageResponse.setId(null);
        messageResponse.setDistributionId(null);
        assertThat(payload, equalTo(messageResponse));
    }

    @Test
    public void messageUndeliveredTest() {
        DeliverSm deliverSm = new DeliverSm();
        deliverSm.setEsmClass(MessageType.SMSC_DEL_RECEIPT.value());
        deliverSm.setShortMessage(
            "id:2 sub:001 dlvrd:001 submit date:0908312310 done date:0908312311 stat:UNDELIV err:xxx Text:Hello SMPP world!"
                .getBytes());
        sendToKafkaDeliveryReportListener.onAcceptDeliverSm(deliverSm);

        ArgumentCaptor<DeliveryReport> argumentCaptor = ArgumentCaptor.forClass(DeliveryReport.class);
        verify(kafkaTemplate).send(eq(FAILED_DELIVERY), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), equalTo(deliveryReport("2", "UNDELIVERABLE")));
    }

    @Test
    public void messageDeliveredTest() {
        DeliverSm deliverSm = new DeliverSm();
        deliverSm.setEsmClass(MessageType.SMSC_DEL_RECEIPT.value());
        deliverSm.setShortMessage(
            "id:111111 sub:001 dlvrd:001 submit date:0908312310 done date:0908312311 stat:DELIVRD err:xxx Text:Hello SMPP world!"
                .getBytes());
        sendToKafkaDeliveryReportListener.onAcceptDeliverSm(deliverSm);

        ArgumentCaptor<DeliveryReport> argumentCaptor = ArgumentCaptor.forClass(DeliveryReport.class);
        verify(kafkaTemplate).send(eq(SUCCESS_DELIVERY), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), equalTo(deliveryReport("1b207", "DELIVERED")));
    }

    @Test
    @SneakyThrows
    public void messageMoDeliveredTest() {
        String messageJson = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("message.json"), UTF_8);

        DeliverSm deliverSm = new DeliverSm();
        deliverSm.setShortMessage("firstMessage".getBytes(ISO_8859_1));
        deliverSm.setDataCoding((byte) 0);

        sendToKafkaMoDeliveryReportListener.onAcceptDeliverSm(deliverSm);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(MO_QUEUE), argumentCaptor.capture());
        JSONAssert.assertEquals(argumentCaptor.getValue(), messageJson.trim(), false);
    }

    @Test
    @SneakyThrows
    public void messageMoDeliveredWithCyrillicTest() {
        String messageJson = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("message-with-cyrillic.json"), UTF_8);

        DeliverSm deliverSm = new DeliverSm();
        deliverSm.setShortMessage("secondMessage с русскими символами".getBytes(UTF_16));
        deliverSm.setDataCoding((byte) 8);

        sendToKafkaMoDeliveryReportListener.onAcceptDeliverSm(deliverSm);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(MO_QUEUE), argumentCaptor.capture());
        JSONAssert.assertEquals(argumentCaptor.getValue(), messageJson.trim(), false);
    }

    public static CommunicationMessage message() {
        CommunicationMessage message = new CommunicationMessage();
        Receiver receiver = new Receiver();
        receiver.setPhoneNumber("PH");
        receiver.setId("ID");
        Sender sender = new Sender();
        sender.setId("TestSender");
        message.setId("TEST_D_ID-SMS-ID");
        message.setSender(sender);
        message.setContent("TestContext");
        message.setReceiver(singletonList(receiver));
        message.setType("SMS");
        message.setCharacteristic(Lists.newArrayList(new CommunicationRequestCharacteristic() {
            {
                name(DELIVERY_REPORT);
                value("1");
            }
        }));
        return message;
    }
}
