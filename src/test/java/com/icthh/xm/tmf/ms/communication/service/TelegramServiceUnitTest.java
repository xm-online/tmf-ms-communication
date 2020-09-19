package com.icthh.xm.tmf.ms.communication.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.dto.DtoMapperConfiguration;
import com.icthh.xm.tmf.ms.communication.dto.InlineKeyboardMarkupDto;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.tmf.ms.communication.config.Constants.REPLY_MARKUP;
import static com.icthh.xm.tmf.ms.communication.dto.InlineKeyboardButtonDto.buildForText;
import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class TelegramServiceUnitTest {

    KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    TelegramBotRegisterService registerService = mock(TelegramBotRegisterService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    TelegramService telegramService = new TelegramService(null, kafkaTemplate, objectMapper, registerService);

    @Before
    public void before() {
        new DtoMapperConfiguration(objectMapper);
    }

    @Test
    @SneakyThrows
    public void testTelegramInlineReplyButton() {
        TelegramBot telegramBot = mock(TelegramBot.class);
        when(registerService.getTenantBots("TEST")).thenReturn(Map.of("botkey", telegramBot));
        CommunicationMessageCreate message = new CommunicationMessageCreate();
        message.setType("botkey");
        message.setContent("mycoolmessage");
        CommunicationRequestCharacteristic characteristic = new CommunicationRequestCharacteristic();
        characteristic.setName(REPLY_MARKUP);
        InlineKeyboardMarkupDto inlineKeyboardMarkupDto = new InlineKeyboardMarkupDto();
        inlineKeyboardMarkupDto.setInlineKeyboard(List.of(
                List.of(buildForText("a1"), buildForText("a2"), buildForText("a3")),
                List.of(buildForText("b1"), buildForText("b2"), buildForText("b3")),
                List.of(buildForText("c1"), buildForText("c2"), buildForText("c3"))
        ));
        characteristic.setValue(objectMapper.writeValueAsString(inlineKeyboardMarkupDto));
        message.setCharacteristic(List.of(characteristic));
        Receiver receiver = new Receiver();
        receiver.setAppUserId("appUserId");
        message.setReceiver(List.of(receiver));
        telegramService.send("TEST", message);
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(messageCaptor.capture());
        Map<String, Object> parameters = messageCaptor.getValue().getParameters();
        assertEquals("mycoolmessage", parameters.get("text"));
        assertEquals("appUserId", parameters.get("chat_id"));
        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) parameters.get("reply_markup");
        InlineKeyboardButton[][] inlineKeyboardButtons = replyMarkup.inlineKeyboard();
        assertEquals(inlineKeyboardButtons[0][0], new InlineKeyboardButton("a1"));
        assertEquals(inlineKeyboardButtons[0][1], new InlineKeyboardButton("a2"));
        assertEquals(inlineKeyboardButtons[0][2], new InlineKeyboardButton("a3"));

        assertEquals(inlineKeyboardButtons[1][0], new InlineKeyboardButton("b1"));
        assertEquals(inlineKeyboardButtons[1][1], new InlineKeyboardButton("b2"));
        assertEquals(inlineKeyboardButtons[1][2], new InlineKeyboardButton("b3"));

        assertEquals(inlineKeyboardButtons[2][0], new InlineKeyboardButton("c1"));
        assertEquals(inlineKeyboardButtons[2][1], new InlineKeyboardButton("c2"));
        assertEquals(inlineKeyboardButtons[2][2], new InlineKeyboardButton("c3"));
    }

}
