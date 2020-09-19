package com.icthh.xm.tmf.ms.communication.dto;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import lombok.Data;

import java.util.List;

@Data
public class InlineKeyboardMarkupDto extends KeyboardDto {

    private List<List<InlineKeyboardButtonDto>> inlineKeyboard;

    @Override
    public Keyboard build() {
        List<List<InlineKeyboardButtonDto>> inlineKeyboard = nullSafe(this.inlineKeyboard);
        InlineKeyboardButton[][] keyboardMarkup = nullSafe(inlineKeyboard).stream()
                .map(InlineKeyboardButtonDto::build)
                .map(l -> nullSafe(l).toArray(new InlineKeyboardButton[0]))
                .toArray(InlineKeyboardButton[][]::new);

        return new InlineKeyboardMarkup(keyboardMarkup);
    }
}
