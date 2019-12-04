package com.icthh.xm.tmf.ms.communication.channel.telegram;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class TelegramUpdateListener implements UpdatesListener {

    private final String tenantKey;
    private final BiConsumer<String, CommunicationMessage> messageConsumer;

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            //todo convert telegram Update to the ms CommunicationMessage
            CommunicationMessage communicationMessage = new CommunicationMessage();
            messageConsumer.accept(tenantKey, communicationMessage);
        });

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
