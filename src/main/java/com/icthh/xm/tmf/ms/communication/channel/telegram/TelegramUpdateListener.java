package com.icthh.xm.tmf.ms.communication.channel.telegram;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class TelegramUpdateListener implements UpdatesListener {

    private final String tenantKey;
    private final BiConsumer<String, CommunicationMessage> messageConsumer;

    @Override
    public int process(List<Update> updates) {
        updates.forEach(this::processUpdate);
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        putRid(update);
        final StopWatch stopWatch = StopWatch.createStarted();

        String rawBody = update.message().text();
        log.info("start processing message, size = {}, body = [{}]", rawBody.length(), rawBody);
        try {
            CommunicationMessage message = convert(update);
            messageConsumer.accept(tenantKey, message);
            log.info("stop processing message, time = {} ms.", stopWatch.getTime());
        } catch (Exception ex) {
            log.error("error processing message, time = {} ms.", stopWatch.getTime());
            throw ex;
        } finally {
            MdcUtils.clear();
        }
    }

    private CommunicationMessage convert(Update update) {
        CommunicationMessage message = new CommunicationMessage();
        message.setContent(update.message().text());

        Sender sender = new Sender();
        sender.setId(update.message().chat().id().toString());
        message.setSender(sender);

        return message;
    }

    private void putRid(Update update) {
        Message message = update.message();
        MdcUtils.putRid(new StringJoiner(":")
            .add(tenantKey)
            .add(message.chat().id().toString())
            .add(message.messageId().toString())
            .toString());
    }
}
