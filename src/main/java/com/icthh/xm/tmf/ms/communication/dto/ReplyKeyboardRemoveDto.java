package com.icthh.xm.tmf.ms.communication.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import lombok.Data;

import static java.lang.Boolean.TRUE;

@Data
@JsonTypeName("replyRemove")
public class ReplyKeyboardRemoveDto extends KeyboardDto {

    private Boolean selective;

    @Override
    public Keyboard build() {
        return new ReplyKeyboardRemove(TRUE.equals(selective));
    }
}
