package com.icthh.xm.tmf.ms.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.lep.LepKafkaMessageHandler;
import com.icthh.xm.tmf.ms.communication.messaging.handler.MessageHandlerService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.icthh.xm.tmf.ms.communication.rules.ttl.TTLRule.MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP;
import static org.apache.commons.lang3.StringUtils.unwrap;
import static org.springframework.kafka.support.KafkaHeaders.ACKNOWLEDGMENT;

@Slf4j
@Service
@ConditionalOnProperty("application.stream-binding-enabled")
public class KafkaHandelMessageService {

    private static final int MICROSECONDS_IN_SEC = 1_000_000;
    public static final String TENANT_NAME = "TENANT.NAME";
    public static final String XM = "XM";
    private static final String SMS_NAME_CHARACTERISTIC = "SMS.PART.COUNT";
    private static final Map<String, String> idCache = new WeakHashMap<>();
    private static final Lock synchronizedLock = new ReentrantLock();

    private final int pauseBetweenSends;
    private final LepKafkaMessageHandler lepMessageHandler;
    private final long kafkaReadSleepTimeout;
    private final MessageHandlerService messageHandlerService;
    private final ObjectMapper objectMapper;
    private volatile AtomicLong nextScheduledTime;

    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    public KafkaHandelMessageService(ApplicationProperties applicationProperties,
                                     MessageHandlerService messageHandlerService,
                                     LepKafkaMessageHandler lepMessageHandler,
                                     ObjectMapper objectMapper) {
        this.messageHandlerService = messageHandlerService;
        this.lepMessageHandler = lepMessageHandler;
        this.objectMapper = objectMapper;

        pauseBetweenSends = MICROSECONDS_IN_SEC / applicationProperties.getKafka().getRateLimit();
        int processingPoolSize = applicationProperties.getKafka().getPoolSize();
        nextScheduledTime = new AtomicLong((System.nanoTime() / 1000) + pauseBetweenSends);
        scheduledExecutorService = Executors.newScheduledThreadPool(processingPoolSize);
        kafkaReadSleepTimeout = processingPoolSize * pauseBetweenSends * 2;
    }

    public void handle(Message<?> message) {
        int pauseBetweenCurrentSend = pauseBetweenSends * getSmsParts(message);
        long delay = nextScheduledTime.getAndAdd(pauseBetweenCurrentSend) - System.nanoTime() / 1000;
        if (delay > kafkaReadSleepTimeout) {
            try {
                //time to sleep in microseconds
                Thread.sleep(kafkaReadSleepTimeout / 1000);
            } catch (InterruptedException e) {
                log.error("error processing kafka sleep timeout", e);
            }
        }
        scheduledExecutorService.schedule(() -> {
            try {
                MdcUtils.putRid(MdcUtils.generateRid());
                handleEvent(message);
            } catch (Exception e) {
                log.error("error processing event", e);
                throw e;
            } finally {
                MdcUtils.removeRid();
            }
        }, delay, TimeUnit.MICROSECONDS);
    }

    private void handleEvent(Message<?> message) {
        final StopWatch stopWatch = StopWatch.createStarted();

        // ACKNOWLEDGMENT before processing (important, for avoid duplicate sms)
        message.getHeaders().get(ACKNOWLEDGMENT, Acknowledgment.class).acknowledge();

        try {
            String payloadString = (String) message.getPayload();
            log.info("start processing message, base64 body = {}, headers = {}", payloadString, getHeaders(message));
            payloadString = unwrap(payloadString, "\"");
            log.info("start processing message, json body = {}", payloadString);
            CommunicationMessage communicationMessage = mapToCommunicationMessage(payloadString);
            try {
                synchronizedLock.lock();
                if (!idCache.containsKey(communicationMessage.getId())) {
                    idCache.put(communicationMessage.getId(), "");
                } else {
                    log.info("Duplicate  message id: {}. Stop process", communicationMessage.getId());
                    return;
                }
            } finally {
                synchronizedLock.unlock();
            }
            lepMessageHandler.preHandler(getTenant(communicationMessage));
            addReceivedByChannelCharacteristic(communicationMessage, message);
            messageHandlerService.getHandler(communicationMessage.getType())
                .handle(communicationMessage);
            log.info("stop processing message, time = {}", stopWatch.getTime());
        } catch (Exception e) {
            log.error("Error process event", e);
        } finally {
            lepMessageHandler.destroy();
        }
    }

    private Map<String, Object> getHeaders(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        Map<String, Object> headersForLog = new HashMap<>(headers);
        headersForLog.remove(ACKNOWLEDGMENT);
        return headersForLog;
    }

    @SneakyThrows
    private CommunicationMessage mapToCommunicationMessage(String eventBody) {
        return objectMapper.readValue(eventBody, CommunicationMessage.class);
    }

    /**
     * Since Kafka headers are not accessible from the business rules,
     * move Kafka received timestamp to the communication message characteristics
     */
    private void addReceivedByChannelCharacteristic(CommunicationMessage communicationMessage, Message<?> kafkaMessage) {
        Optional.ofNullable(kafkaMessage)
            .map(Message::getHeaders)
            .map(headers -> headers.get(KafkaHeaders.RECEIVED_TIMESTAMP))
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .ifPresent(kafkaReceivedTimestamp ->
                communicationMessage.addCharacteristicItem(
                    new CommunicationRequestCharacteristic()
                        // Rename it to unlink name from source channel
                        .name(MESSAGE_RECEIVED_BY_CHANNEL_TIMESTAMP)
                        .value(kafkaReceivedTimestamp)
                )
            );
    }

    private String getTenant(CommunicationMessage message) {
        return message.getCharacteristic().stream().filter(ch -> TENANT_NAME.equals(ch.getName())).findFirst()
            .map(ch -> ch.getValue()).orElse(XM);
    }

    private CommunicationMessage getMessage(Message<?> message) {
        String payloadString = (String) message.getPayload();
        payloadString = unwrap(payloadString, "\"");
        return mapToCommunicationMessage(payloadString);
    }

    public int getSmsParts(Message<?> message) {
        return Integer.parseInt(getMessage(message).getCharacteristic()
            .stream()
            .filter(characteristic -> characteristic.getName().equals(SMS_NAME_CHARACTERISTIC))
            .findAny().orElseGet(() -> {
                CommunicationRequestCharacteristic characteristic = new CommunicationRequestCharacteristic();
                characteristic.setValue("1");
                return characteristic;
            }).getValue());
    }
}
