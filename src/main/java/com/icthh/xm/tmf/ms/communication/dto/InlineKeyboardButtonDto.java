package com.icthh.xm.tmf.ms.communication.dto;

import com.pengrad.telegrambot.model.request.CallbackGame;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.LoginUrl;
import lombok.Data;
import org.apache.kafka.common.security.auth.Login;

import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;

@Data
public class InlineKeyboardButtonDto {

    private String text;
    private String url;
    private LoginUrlDto loginUrl;
    private String callbackData;
    private String switchInlineQuery;
    private String switchInlineQueryCurrentChat;
    private Boolean pay;

    public static List<InlineKeyboardButton> build(List<InlineKeyboardButtonDto> inlineKeyboardButtons) {
        return inlineKeyboardButtons.stream().map(InlineKeyboardButtonDto::build).collect(toList());
    }

    public InlineKeyboardButton build() {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(text)
                .url(url)
                .loginUrl(loginUrl != null ? loginUrl.build() : null)
                .callbackData(callbackData)
                .switchInlineQuery(switchInlineQuery)
                .switchInlineQueryCurrentChat(switchInlineQueryCurrentChat);

        if (TRUE.equals(pay)) {
            inlineKeyboardButton.pay();
        }
        return inlineKeyboardButton;
    }

    @Data
    public static class LoginUrlDto {
        private String url;
        private String forwardText;
        private String botUsername;
        private Boolean requestWriteAccess;

        public LoginUrl build() {
            return new LoginUrl(url)
                    .forwardText(forwardText)
                    .botUsername(botUsername)
                    .requestWriteAccess(TRUE.equals(requestWriteAccess));
        }
    }

    public static InlineKeyboardButtonDto buildForText(String text) {
        InlineKeyboardButtonDto keyboardButtonDto = new InlineKeyboardButtonDto();
        keyboardButtonDto.text = text;
        return keyboardButtonDto;
    }

}
