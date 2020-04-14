package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.messaging.MessagingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Service
@Slf4j
public class KafkaHandelMessageService {

    private static final int MICROSECONDS_IN_SEC = 1_000_000;
    private final int pauseBetweenSends;
    private final long kafkaReadSleepTimeout;
    private final MessagingHandler messagingHandler;
    private volatile AtomicLong nextScheduledTime;

    private ScheduledExecutorService scheduledExecutorService;

    public KafkaHandelMessageService(ApplicationProperties applicationProperties,
                                     MessagingHandler messagingHandler) {
        this.messagingHandler = messagingHandler;
        pauseBetweenSends = MICROSECONDS_IN_SEC / applicationProperties.getKafka().getRateLimit();
        int processingPoolSize = applicationProperties.getKafka().getPoolSize();
        nextScheduledTime = new AtomicLong((System.nanoTime() / 1000) + pauseBetweenSends);
        scheduledExecutorService = Executors.newScheduledThreadPool(processingPoolSize);
        kafkaReadSleepTimeout = processingPoolSize * pauseBetweenSends * 2;
    }

    public void handle(Message<?> message) {

        long delay = nextScheduledTime.getAndAdd(pauseBetweenSends) - System.nanoTime() / 1000;
        if (delay > kafkaReadSleepTimeout) {
            try {
                //time to sleep in microseconds
                Thread.sleep(kafkaReadSleepTimeout/1000);
            } catch (InterruptedException e) {
                log.error("error processing kafka sleep timeout", e);
            }
        }
        scheduledExecutorService.schedule(() -> {
            try {
                MdcUtils.putRid(MdcUtils.generateRid());
                messagingHandler.handleEvent(message);
            } catch (Exception e) {
                log.error("error processing event", e);
                throw e;
            } finally {
                MdcUtils.removeRid();
            }
        }, delay, TimeUnit.MICROSECONDS);
    }

}
