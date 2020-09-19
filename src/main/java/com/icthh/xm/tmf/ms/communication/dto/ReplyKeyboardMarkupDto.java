package com.icthh.xm.tmf.ms.communication.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import lombok.Data;

import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;

@Data
@JsonTypeName("reply")
public class ReplyKeyboardMarkupDto extends KeyboardDto {

    private List<List<KeyboardButtonDto>> keyboard;
    private Boolean resizeKeyboard;
    private Boolean oneTimeKeyboard;
    private Boolean selective;

    @Override
    public Keyboard build() {
        List<List<KeyboardButtonDto>> inlineKeyboard = nullSafe(this.keyboard);
        KeyboardButton[][] keyboardMarkup = nullSafe(inlineKeyboard).stream()
                .map(KeyboardButtonDto::build)
                .map(l -> nullSafe(l).toArray(new KeyboardButton[0]))
                .toArray(KeyboardButton[][]::new);

        return new ReplyKeyboardMarkup(keyboardMarkup)
                .oneTimeKeyboard(TRUE.equals(oneTimeKeyboard))
                .resizeKeyboard(TRUE.equals(resizeKeyboard))
                .selective(TRUE.equals(selective));
    }

    @Data
    public static class KeyboardButtonDto {
        private String text;
        private Boolean requestContact;
        private Boolean requestLocation;

        public static List<KeyboardButton> build(List<KeyboardButtonDto> keyboardButtons) {
            return keyboardButtons.stream().map(KeyboardButtonDto::build).collect(toList());
        }

        public KeyboardButton build() {
            return new KeyboardButton(text)
                    .requestContact(TRUE.equals(requestContact))
                    .requestLocation(TRUE.equals(requestLocation));
        }
    }
}
