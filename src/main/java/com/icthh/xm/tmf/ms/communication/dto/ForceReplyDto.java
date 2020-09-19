package com.icthh.xm.tmf.ms.communication.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.Keyboard;
import lombok.Data;

import static java.lang.Boolean.TRUE;

@Data
@JsonTypeName("force")
public class ForceReplyDto extends KeyboardDto {
    private Boolean selective;

    @Override
    public Keyboard build() {
        return new ForceReply(TRUE.equals(selective));
    }
}
