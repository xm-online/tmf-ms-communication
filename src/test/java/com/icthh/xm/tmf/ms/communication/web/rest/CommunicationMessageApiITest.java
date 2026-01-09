package com.icthh.xm.tmf.ms.communication.web.rest;

import com.google.common.collect.ImmutableMap;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageCreateResolver;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.messaging.handler.CustomCommunicationMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.EmailMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MobileAppMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.TwilioMessageHandler;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiController;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.CustomExceptionTranslator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.CollectionHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.tmf.ms.communication.domain.MessageType.Email;
import static com.icthh.xm.tmf.ms.communication.domain.MessageType.MobileApp;
import static com.icthh.xm.tmf.ms.communication.domain.MessageType.Twilio;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = CommunicationMessageApiController.class)
@ContextConfiguration(classes = {
    CommunicationMessageApiImpl.class,
    CommunicationMessageApiController.class,
    CustomExceptionTranslator.class,
    MessageHandlerService.class,
    CustomCommunicationMessageHandler.class,
    CustomMessageResolver.class,
    CustomMessageCreateResolver.class
})
public class CommunicationMessageApiITest {

    public static final String CONTEXT_OF_SMS = "Context of sms";
    public static final String SENDER_ID = "SENDER_ID";
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    SmppMessagingHandler smppMessagingHandler;

    @MockBean
    TwilioMessageHandler twilioMessageHandler;

    @MockBean
    EmailMessageHandler emailMessageHandler;

    @MockBean
    private MobileAppMessageHandler mobileAppMessageHandler;


    @Test
    @SneakyThrows
    public void testCreatesANewMessageWithFreemarkerKeySubject() {
        Map<String, Object> request = of(
                "content", CONTEXT_OF_SMS,
                "receiver", createReceivers("phoneNumber", "380900510000", "380900510001", "380900510002"),
                "type", "SMS",
                "sender", of("id", SENDER_ID),
                "subject", of("en", "")
        );

        mockMvc.perform(
                        post("/api/communicationManagement/v2/communicationMessage/send").contentType("application/json")
                                .content(TestUtil.convertObjectToJsonBytes(
                                        request)))
                .andDo(print())
                .andExpect(status().isOk());

        ArgumentCaptor<CommunicationMessageCreate> captor = ArgumentCaptor.forClass(CommunicationMessageCreate.class);
        Set<String> phones = CollectionHelper.asSet("380900510000", "380900510001", "380900510002");
        verify(smppMessagingHandler).handle(captor.capture());
        assertEquals(captor.getValue().getSender().getId(), SENDER_ID);
        assertEquals(captor.getValue().getType(), "SMS");
        assertTrue(captor.getValue().getReceiver().stream().allMatch(r -> phones.contains(r.getPhoneNumber())));
    }

    @Test
    @SneakyThrows
    public void testCreatesANewCommunicationMessageAndSendIt() {
        Map<String, Object> request = of(
            "content", CONTEXT_OF_SMS,
            "receiver", createReceivers("phoneNumber", "380900510000", "380900510001", "380900510002"),
            "type", "SMS",
            "sender", of("id", SENDER_ID)
        );

        mockMvc.perform(
            post("/api/communicationManagement/v2/communicationMessage/send").contentType("application/json")
                .content(TestUtil.convertObjectToJsonBytes(
                    request)))
            .andDo(print())
            .andExpect(status().isOk());

        ArgumentCaptor<CommunicationMessageCreate> captor = ArgumentCaptor.forClass(CommunicationMessageCreate.class);
        Set<String> phones = CollectionHelper.asSet("380900510000", "380900510001", "380900510002");
        verify(smppMessagingHandler).handle(captor.capture());
        assertEquals(captor.getValue().getSender().getId(), SENDER_ID);
        assertEquals(captor.getValue().getType(), "SMS");
        assertTrue(captor.getValue().getReceiver().stream().allMatch(r -> phones.contains(r.getPhoneNumber())));
    }

    @Test
    @SneakyThrows
    public void testCreatesANewPushMessageAndSendIt() {
        Map<String, Object> request = of(
            "content", CONTEXT_OF_SMS,
            "receiver", createReceivers("appUserId", "111111"),
            "type", "MobileApp",
            "sender", of("id", SENDER_ID)
        );

        mockMvc.perform(
            post("/api/communicationManagement/v2/communicationMessage/send").contentType("application/json")
                .content(TestUtil.convertObjectToJsonBytes(
                    request)))
            .andDo(print())
            .andExpect(status().isOk());


        ArgumentCaptor<CommunicationMessageCreate> captor = ArgumentCaptor.forClass(CommunicationMessageCreate.class);
        verify(mobileAppMessageHandler).handle(captor.capture());

        assertThat(captor.getValue().getReceiver().get(0).getAppUserId(), equalTo("111111"));
        assertThat(captor.getValue().getType(), equalTo(MobileApp.name()));
    }

    @Test
    @SneakyThrows
    public void testCreatesANewEmailMessageAndSendIt() {
        Map<String, Object> request = of(
            "content", CONTEXT_OF_SMS,
            "receiver", createReceivers("appUserId", "111111"),
            "type", "Email",
            "sender", of("id", SENDER_ID)
        );

        mockMvc.perform(
            post("/api/communicationManagement/v2/communicationMessage/send").contentType("application/json")
                .content(TestUtil.convertObjectToJsonBytes(
                    request)))
            .andDo(print())
            .andExpect(status().isOk());

        ArgumentCaptor<CommunicationMessageCreate> captor = ArgumentCaptor.forClass(CommunicationMessageCreate.class);
        verify(mobileAppMessageHandler).handle(captor.capture());

        assertThat(captor.getValue().getReceiver().get(0).getAppUserId(), equalTo("111111"));
        assertThat(captor.getValue().getType(), equalTo(Email.name()));
    }

    @Test
    @SneakyThrows
    public void testCreateTwilioMessageAndSendIt() {

        Map<String, Object> request = of("content", CONTEXT_OF_SMS, "receiver",
            createReceivers("email", "email@mail.com"), "type", "Twilio",
            "sender", of("id", SENDER_ID));

        mockMvc.perform(
            post("/api/communicationManagement/v2/communicationMessage/send").contentType("application/json")
                .content(TestUtil.convertObjectToJsonBytes(
                    request)))
            .andDo(print())
            .andExpect(status().isOk());


        ArgumentCaptor<CommunicationMessageCreate> captor = ArgumentCaptor.forClass(CommunicationMessageCreate.class);
        verify(emailMessageHandler).handle(captor.capture());

        CommunicationMessageCreate value = captor.getValue();
        assertThat(value.getReceiver().get(0).getEmail(), equalTo("email@mail.com"));
        assertThat(value.getType(), equalTo(Twilio.name()));
    }


    private List<ImmutableMap<String, String>> createReceivers(String propertyName, String... values) {
        return Arrays.stream(values).map(it -> of(propertyName, it, "id", it)).collect(toList());
    }
}
