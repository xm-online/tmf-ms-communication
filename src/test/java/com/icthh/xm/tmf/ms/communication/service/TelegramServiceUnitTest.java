package com.icthh.xm.tmf.ms.communication.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TelegramServiceUnitTest {

    private static final String TENANT_KEY = "TEST";

    @Spy
    @InjectMocks
    private TelegramService telegramService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void botExecute() {
        TelegramBot telegramBot = Mockito.mock(TelegramBot.class);

        Receiver receiver = new Receiver();
        receiver.setAppUserId("chatId");

        CommunicationRequestCharacteristic characteristic = new CommunicationRequestCharacteristic().name("keyboardMarkup")
            .value("[[{\"name\": \"a\"},{\"name\": \"b\"},{\"name\": \"c\"}],[{\"name\": \"d\"}]]");

        CommunicationMessageCreate message = new CommunicationMessageCreate();
        message.setContent("content");
        message.setType("type");
        message.setCharacteristic(List.of(characteristic));

        SendMessage sendMessage = prepareMessage(receiver.getAppUserId(), message.getContent(), message.getCharacteristic().get(0));
        telegramService.botExecute(telegramBot, receiver, message);

        verify(telegramBot).execute(argThat((ArgumentMatcher<SendMessage>) argument -> (argument)
            .getParameters().get("chat_id").equals(sendMessage.getParameters().get("chat_id"))));

    }

    private SendMessage prepareMessage(String chatId, String content, CommunicationRequestCharacteristic characteristic) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<List<LinkedHashMap<String, String>>> keyboardListModel = null;
        try {
            keyboardListModel = objectMapper.readValue(characteristic.getValue(), new TypeReference<List<List<LinkedHashMap<String, String>>>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[][] keyboardMarkup = keyboardListModel.stream()
            .map(arr -> arr.stream().map(it -> it.get("name")).collect(Collectors.toList()))
            .map(arrString -> arrString.toArray(String[]::new))
            .toArray(String[][]::new);
        Keyboard replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardMarkup);

        return new SendMessage(chatId, content)
            .replyMarkup(replyKeyboardMarkup);
    }

}
