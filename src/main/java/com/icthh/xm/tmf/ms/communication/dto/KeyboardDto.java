package com.icthh.xm.tmf.ms.communication.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pengrad.telegrambot.model.request.Keyboard;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
public abstract class KeyboardDto {

    public abstract Keyboard build();

    protected <T> List<T> nullSafe(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

}
