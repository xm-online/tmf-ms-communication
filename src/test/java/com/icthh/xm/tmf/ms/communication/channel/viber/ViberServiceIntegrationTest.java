package com.icthh.xm.tmf.ms.communication.channel.viber;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.google.gson.Gson;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.config.InfobipViberConfig;
import com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.service.ViberConfigGetter;
import com.icthh.xm.tmf.ms.communication.config.LepConfiguration;
import com.icthh.xm.tmf.ms.communication.config.RestTemplateConfiguration;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.domain.DeliveryReport;
import com.icthh.xm.tmf.ms.communication.domain.MessageResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest()
@RunWith(SpringRunner.class)
@ContextConfiguration(
    classes = {CommunicationApp.class,
        SecurityBeanOverrideConfiguration.class,
        LepConfiguration.class,
        RestTemplateConfiguration.class,
        TestTopicCreationConfiguration.class,
        TestKafkaProducer.class,
        TestKafkaListener.class})
//@ContextConfiguration(initializers = KafkaContextInitializer.class)
@TestPropertySource(properties = {
    "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.consumer.value-serializer=org.apache.kafka.common.serialization.StringSerializerArrayListValuedHashMap",
    "spring.kafka.consumer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "application.messaging.to-send-queue-name=communication_to_send_sms",
    "application.messaging.sent-queue-name=communication_sent_sms",
    "application.messaging.send-failed-queue-name=communication_failed_sms",
    "application.messaging.delivery-failed-queue-name=communication_delivery_failed",
    "application.messaging.delivered-queue-name=communication_delivered_reports",
})
//)
public class ViberServiceIntegrationTest {

    private static final Gson gson = new Gson();

    @MockBean
    private ViberConfigGetter viberConfigGetter;

    //@ClassRule
//    public static KafkaContainer kafka =
//        KafkaContainerStaticKeeper.keepContainer(
//            new KafkaContainer()
//                .withExposedPorts(9092)
//                .withExposedPorts(9093));

    @MockBean
    private SmppService smppMessagingHandler;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8888), false);

    @Autowired
    private TestKafkaProducer testKafkaProducer;

    @Autowired
    private TestKafkaListener testKafkaListener;

    @Before
    public void prepare() {
        given(viberConfigGetter.apply(any()))
            .willReturn(
                new InfobipViberConfig(
                    format("http://localhost:%s", wireMockRule.port()),
                    "Basic 123", "test_scenario_key"));
    }

    @Test
    public void message_should_be_generated_when_successfully_sent_message() throws InterruptedException {
        //GIVEN
        stubFor(WireMock.post(urlEqualTo("/omni/1/advanced"))
            .withHeader("Authorization", equalTo("Basic 123"))
            .withRequestBody(equalToJson(
                "{\n" +
                    "  \"messageId\": \"test_message_id_1\",\n" +
                    "  \"scenarioKey\": \"test_scenario_key\",\n" +
                    "  \"destinations\": [\n" +
                    "    {\n" +
                    "      \"to\": {\n" +
                    "        \"phoneNumber\": \"380501111111\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"viber\": {\n" +
                    "    \"text\" : \"test viber text message\",\n" +
                    "    \"imageURL\": \"test image url\",\n" +
                    "    \"buttonText\": \"test button text\",\n" +
                    "    \"buttonURL\": \"test button url\"\n" +
                    "  }\n" +
                    "}"
            ))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody("{\n" +
                    "  \"messages\": [\n" +
                    "    {\n" +
                    "      \"messageId\": \"test_message_id_1\",\n" +
                    "      \"to\": {\n" +
                    "        \"phoneNumber\": \"380501111111\"\n" +
                    "      },\n" +
                    "      \"status\": {\n" +
                    "        \"groupId\": 1\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n")));
        stubEmptyReports();

        MultiValueMap<String, String> producedRawMessages = new LinkedMultiValueMap<>();
        testKafkaListener.addListener(producedRawMessages::add);

        //WHEN
        CommunicationMessage communicationMessage = new CommunicationMessage();
        communicationMessage.setContent("test viber text message");
        communicationMessage.setId("test_message_id_1");
        communicationMessage.setType("Viber");
        Receiver receiver = new Receiver();
        receiver.setId("380501111111");
        receiver.setPhoneNumber("380501111111");
        communicationMessage.addReceiverItem(receiver);
        Sender sender = new Sender();
        sender.setId("test_sender_id");
        sender.setName("test sender name");
        communicationMessage.setSender(sender);
        communicationMessage.setCharacteristic(new ArrayList<>());
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_TEXT").value("test button text"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_URL").value("test button url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_IMAGE_URL").value("test image url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("DISTRIBUTION.ID").value("distribution_id_1"));

        testKafkaProducer.sendMessage("communication_to_send_sms", communicationMessage);

        TimeUnit.SECONDS.sleep(1);

        //THEN
        MultiValueMap<String, Object> expectedMessages = new LinkedMultiValueMap<>();
        MessageResponse value = new MessageResponse(MessageResponse.Status.SUCCESS, communicationMessage);
        value.setMessageId("test_message_id_1");
        expectedMessages.add("communication_sent_sms", value);

        MultiValueMap<String, Object> actualMessages = getActualMessages(producedRawMessages);

        Assert.assertEquals(expectedMessages, actualMessages);
    }

    @Test
    public void message_should_be_generated_when_sending_failed_due_to_infobip_500_error() throws InterruptedException {
        //GIVEN
        stubFor(WireMock.post(urlEqualTo("/omni/1/advanced"))
            .withHeader("Authorization", equalTo("Basic 123"))
            .withRequestBody(equalToJson(
                "{\n" +
                    "  \"messageId\": \"test_message_id_1\",\n" +
                    "  \"scenarioKey\": \"test_scenario_key\",\n" +
                    "  \"destinations\": [\n" +
                    "    {\n" +
                    "      \"to\": {\n" +
                    "        \"phoneNumber\": \"380501111111\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"viber\": {\n" +
                    "    \"text\" : \"test viber text message\",\n" +
                    "    \"imageURL\": \"test image url\",\n" +
                    "    \"buttonText\": \"test button text\",\n" +
                    "    \"buttonURL\": \"test button url\"\n" +
                    "  }\n" +
                    "}"
            ))
            .willReturn(aResponse().withStatus(500)));
        stubEmptyReports();

        MultiValueMap<String, String> producedRawMessages = new LinkedMultiValueMap<>();
        testKafkaListener.addListener(producedRawMessages::add);

        //WHEN
        CommunicationMessage communicationMessage = new CommunicationMessage();
        communicationMessage.setContent("test viber text message");
        communicationMessage.setId("test_message_id_1");
        communicationMessage.setType("Viber");
        Receiver receiver = new Receiver();
        receiver.setId("380501111111");
        receiver.setPhoneNumber("380501111111");
        communicationMessage.addReceiverItem(receiver);
        Sender sender = new Sender();
        sender.setId("test_sender_id");
        sender.setName("test sender name");
        communicationMessage.setSender(sender);
        communicationMessage.setCharacteristic(new ArrayList<>());
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_TEXT").value("test button text"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_URL").value("test button url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_IMAGE_URL").value("test image url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("DISTRIBUTION.ID").value("distribution_id_1"));

        testKafkaProducer.sendMessage("communication_to_send_sms", communicationMessage);

        TimeUnit.SECONDS.sleep(1);

        //THEN
        MultiValueMap<String, Object> expectedMessages = new LinkedMultiValueMap<>();
        MessageResponse expectedMessageResponse = new MessageResponse(MessageResponse.Status.FAILED, communicationMessage);
        expectedMessageResponse.setErrorCode("error.system.sending.viber.gateway.internalError");
        expectedMessageResponse.setErrorMessage("Viber provider responded with 500 http code");
        expectedMessages.add("communication_failed_sms", expectedMessageResponse);

        MultiValueMap<String, Object> actualMessages = getActualMessages(producedRawMessages);

        Assert.assertEquals(expectedMessages, actualMessages);
    }

    @Test
    public void message_should_be_generated_when_sending_failed_due_to_infobip_503_error() throws InterruptedException {
        //GIVEN
        stubFor(WireMock.post(urlEqualTo("/omni/1/advanced"))
            .withHeader("Authorization", equalTo("Basic 123"))
            .withRequestBody(equalToJson(
                "{\n" +
                    "  \"messageId\": \"test_message_id_1\",\n" +
                    "  \"scenarioKey\": \"test_scenario_key\",\n" +
                    "  \"destinations\": [\n" +
                    "    {\n" +
                    "      \"to\": {\n" +
                    "        \"phoneNumber\": \"380501111111\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"viber\": {\n" +
                    "    \"text\" : \"test viber text message\",\n" +
                    "    \"imageURL\": \"test image url\",\n" +
                    "    \"buttonText\": \"test button text\",\n" +
                    "    \"buttonURL\": \"test button url\"\n" +
                    "  }\n" +
                    "}"
            ))
            .willReturn(aResponse().withStatus(503)));
        stubEmptyReports();

        MultiValueMap<String, String> producedRawMessages = new LinkedMultiValueMap<>();
        testKafkaListener.addListener(producedRawMessages::add);

        //WHEN
        CommunicationMessage communicationMessage = new CommunicationMessage();
        communicationMessage.setContent("test viber text message");
        communicationMessage.setId("test_message_id_1");
        communicationMessage.setType("Viber");
        Receiver receiver = new Receiver();
        receiver.setId("380501111111");
        receiver.setPhoneNumber("380501111111");
        communicationMessage.addReceiverItem(receiver);
        Sender sender = new Sender();
        sender.setId("test_sender_id");
        sender.setName("test sender name");
        communicationMessage.setSender(sender);
        communicationMessage.setCharacteristic(new ArrayList<>());
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_TEXT").value("test button text"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_URL").value("test button url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_IMAGE_URL").value("test image url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("DISTRIBUTION.ID").value("distribution_id_1"));

        testKafkaProducer.sendMessage("communication_to_send_sms", communicationMessage);

        TimeUnit.SECONDS.sleep(1);

        //THEN
        MultiValueMap<String, Object> expectedMessages = new LinkedMultiValueMap<>();
        MessageResponse expectedMessageResponse = new MessageResponse(MessageResponse.Status.FAILED, communicationMessage);
        expectedMessageResponse.setErrorCode("error.system.sending.viber.gateway.internalError");
        expectedMessageResponse.setErrorMessage("Viber provider responded with 503 http code");
        expectedMessages.add("communication_failed_sms", expectedMessageResponse);

        MultiValueMap<String, Object> actualMessages = getActualMessages(producedRawMessages);

        Assert.assertEquals(expectedMessages, actualMessages);
    }

    @Test
    public void message_should_be_generated_when_sending_failed_due_to_message_rejection() throws InterruptedException {
        //GIVEN
        stubFor(WireMock.post(urlEqualTo("/omni/1/advanced"))
            .withHeader("Authorization", equalTo("Basic 123"))
            .withRequestBody(equalToJson(
                "{\n" +
                    "  \"messageId\": \"test_message_id_1\",\n" +
                    "  \"scenarioKey\": \"test_scenario_key\",\n" +
                    "  \"destinations\": [\n" +
                    "    {\n" +
                    "      \"to\": {\n" +
                    "        \"phoneNumber\": \"380501111111\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"viber\": {\n" +
                    "    \"text\" : \"test viber text message\",\n" +
                    "    \"imageURL\": \"test image url\",\n" +
                    "    \"buttonText\": \"test button text\",\n" +
                    "    \"buttonURL\": \"test button url\"\n" +
                    "  }\n" +
                    "}"
            ))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody("{\n" +
                    "  \"messages\": [\n" +
                    "    {\n" +
                    "      \"messageId\": \"test_message_id_1\",\n" +
                    "      \"to\": {\n" +
                    "        \"phoneNumber\": \"380501111111\"\n" +
                    "      },\n" +
                    "      \"status\": {\n" +
                    "        \"groupId\": 5,\n" +
                    "        \"description\": \"message rejected\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n")));
        stubEmptyReports();

        MultiValueMap<String, String> producedRawMessages = new LinkedMultiValueMap<>();
        testKafkaListener.addListener(producedRawMessages::add);

        //WHEN
        CommunicationMessage communicationMessage = new CommunicationMessage();
        communicationMessage.setContent("test viber text message");
        communicationMessage.setId("test_message_id_1");
        communicationMessage.setType("Viber");
        Receiver receiver = new Receiver();
        receiver.setId("380501111111");
        receiver.setPhoneNumber("380501111111");
        communicationMessage.addReceiverItem(receiver);
        Sender sender = new Sender();
        sender.setId("test_sender_id");
        sender.setName("test sender name");
        communicationMessage.setSender(sender);
        communicationMessage.setCharacteristic(new ArrayList<>());
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_TEXT").value("test button text"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_BUTTON_URL").value("test button url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("VIBER_IMAGE_URL").value("test image url"));
        communicationMessage.addCharacteristicItem(new CommunicationRequestCharacteristic().name("DISTRIBUTION.ID").value("distribution_id_1"));

        testKafkaProducer.sendMessage("communication_to_send_sms", communicationMessage);

        TimeUnit.SECONDS.sleep(1);

        //THEN
        MultiValueMap<String, Object> expectedMessages = new LinkedMultiValueMap<>();
        MessageResponse expectedMessageResponse = new MessageResponse(MessageResponse.Status.FAILED, communicationMessage);
        expectedMessageResponse.setErrorCode("error.system.sending.viber.gateway.rejection");
        expectedMessageResponse.setErrorMessage("message rejected");
        expectedMessages.add("communication_failed_sms", expectedMessageResponse);

        MultiValueMap<String, Object> actualMessages = getActualMessages(producedRawMessages);

        Assert.assertEquals(expectedMessages, actualMessages);
    }

    @Test
    public void message_should_be_generated_when_obtained_report_with_delivered_status() throws InterruptedException {
        //GIVEN
        stubFor(WireMock.get(urlEqualTo("/omni/1/reports"))
            .inScenario("reports_polling")
            .withHeader("Authorization", equalTo("Basic 123"))
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("after_polling")
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withStatus(200)
                .withBody("{\n" +
                    "  \"results\": [\n" +
                    "    {\n" +
                    "      \"messageId\": \"test_message_id_1\",\n" +
                    "      \"status\": {\n" +
                    "        \"groupId\": 3\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n")
            )
        );

        stubFor(WireMock.get(urlEqualTo("/omni/1/reports"))
            .inScenario("reports_polling")
            .withHeader("Authorization", equalTo("Basic 123"))
            .whenScenarioStateIs("after_polling")
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withStatus(200)
                .withBody("{\n" +
                    "  \"results\": []\n" +
                    "}\n")
            )
        );

        MultiValueMap<String, String> producedRawMessages = new LinkedMultiValueMap<>();
        testKafkaListener.addListener(producedRawMessages::add);

        //WHEN

        //wait until messages are acquired by communication
        TimeUnit.MILLISECONDS.sleep(1000);

        //THEN
        MultiValueMap<String, Object> expectedMessages = new LinkedMultiValueMap<>();
        DeliveryReport expectedDeliveryReport = DeliveryReport.deliveryReport("test_message_id_1", "DELIVERED");
        expectedMessages.add("communication_delivered_reports", expectedDeliveryReport);

        MultiValueMap<String, Object> actualMessages = getActualMessages(producedRawMessages);

        Assert.assertEquals(expectedMessages, actualMessages);
    }

    @Test
    public void message_should_be_generated_when_obtained_report_with_undeliverable_status() throws InterruptedException {
        //GIVEN
        stubFor(WireMock.get(urlEqualTo("/omni/1/reports"))
            .inScenario("reports_polling")
            .withHeader("Authorization", equalTo("Basic 123"))
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("after_polling")
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withStatus(200)
                .withBody("{\n" +
                    "  \"results\": [\n" +
                    "    {\n" +
                    "      \"messageId\": \"test_message_id_1\",\n" +
                    "      \"status\": {\n" +
                    "        \"groupId\": 2\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n")
            )
        );

        stubFor(WireMock.get(urlEqualTo("/omni/1/reports"))
            .withHeader("Authorization", equalTo("Basic 123"))
            .inScenario("reports_polling")
            .whenScenarioStateIs("after_polling")
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withStatus(200)
                .withBody("{\n" +
                    "  \"results\": []\n" +
                    "}\n")
            )
        );

        MultiValueMap<String, String> producedRawMessages = new LinkedMultiValueMap<>();
        testKafkaListener.addListener(producedRawMessages::add);

        //WHEN

        //wait until messages are acquired by communication
        TimeUnit.MILLISECONDS.sleep(1000);

        //THEN
        MultiValueMap<String, Object> expectedMessages = new LinkedMultiValueMap<>();
        DeliveryReport expectedDeliveryReport = DeliveryReport.deliveryReport("test_message_id_1", "UNDELIVERABLE");
        expectedMessages.add("communication_delivery_failed", expectedDeliveryReport);

        MultiValueMap<String, Object> actualMessages = getActualMessages(producedRawMessages);

        Assert.assertEquals(expectedMessages, actualMessages);
    }

    @Test
    public void message_should_be_generated_when_obtained_report_with_expired_status() throws InterruptedException {
        //GIVEN
        stubFor(WireMock.get(urlEqualTo("/omni/1/reports"))
            .inScenario("reports_polling")
            .withHeader("Authorization", equalTo("Basic 123"))
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("after_polling")
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withStatus(200)
                .withBody("{\n" +
                    "  \"results\": [\n" +
                    "    {\n" +
                    "      \"messageId\": \"test_message_id_1\",\n" +
                    "      \"status\": {\n" +
                    "        \"groupId\": 4\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n")
            )
        );

        stubFor(WireMock.get(urlEqualTo("/omni/1/reports"))
            .withHeader("Authorization", equalTo("Basic 123"))
            .inScenario("reports_polling")
            .whenScenarioStateIs("after_polling")
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withStatus(200)
                .withBody("{\n" +
                    "  \"results\": []\n" +
                    "}\n")
            )
        );

        MultiValueMap<String, String> producedRawMessages = new LinkedMultiValueMap<>();
        testKafkaListener.addListener(producedRawMessages::add);

        //WHEN

        //wait until messages are acquired by communication
        TimeUnit.MILLISECONDS.sleep(1500);

        //THEN
        MultiValueMap<String, Object> expectedMessages = new LinkedMultiValueMap<>();
        DeliveryReport expectedDeliveryReport = DeliveryReport.deliveryReport("test_message_id_1", "EXPIRED");
        expectedMessages.add("communication_delivery_failed", expectedDeliveryReport);

        MultiValueMap<String, Object> actualMessages = getActualMessages(producedRawMessages);

        Assert.assertEquals(expectedMessages, actualMessages);
    }

    private void stubEmptyReports() {
        stubFor(WireMock.get(urlEqualTo("/omni/1/reports"))
            .withHeader("Authorization", equalTo("Basic 123"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withStatus(200)
                .withBody("{\n" +
                    "  \"results\": []\n" +
                    "}\n")
            )
        );
    }

    @NotNull
    private MultiValueMap<String, Object> getActualMessages(MultiValueMap<String, String> producedRawMessages) {
        MultiValueMap<String, Object> actualMessages = new LinkedMultiValueMap<>();

        if (producedRawMessages.get("communication_sent_sms") != null) {
            for (String communicationSentSms : Objects.requireNonNull(producedRawMessages.get("communication_sent_sms"))) {
                MessageResponse mr = gson.fromJson(communicationSentSms, MessageResponse.class);
                mr.getResponseTo().setCharacteristic(mr.getResponseTo().getCharacteristic()
                    .stream()
                    .filter(characteristic -> !characteristic.getName().equals("MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP"))
                    .collect(Collectors.toList()));

                actualMessages.add("communication_sent_sms", mr);
            }
        }
        if (producedRawMessages.get("communication_failed_sms") != null) {
            for (String communicationSentSms : Objects.requireNonNull(producedRawMessages.get("communication_failed_sms"))) {
                MessageResponse mr = gson.fromJson(communicationSentSms, MessageResponse.class);
                mr.getResponseTo().setCharacteristic(mr.getResponseTo().getCharacteristic()
                    .stream()
                    .filter(characteristic -> !characteristic.getName().equals("MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP"))
                    .collect(Collectors.toList()));

                actualMessages.add("communication_failed_sms", mr);
            }
        }
        if (producedRawMessages.get("communication_delivered_reports") != null) {
            for (String communicationSentSms : Objects.requireNonNull(producedRawMessages.get("communication_delivered_reports"))) {
                actualMessages.add("communication_delivered_reports", gson.fromJson(communicationSentSms, DeliveryReport.class));
            }
        }
        if (producedRawMessages.get("communication_delivery_failed") != null) {
            for (String communicationSentSms : Objects.requireNonNull(producedRawMessages.get("communication_delivery_failed"))) {
                actualMessages.add("communication_delivery_failed", gson.fromJson(communicationSentSms, DeliveryReport.class));
            }
        }
        return actualMessages;
    }

}
