package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.commons.lep.TenantScriptStorage;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * Properties specific to Communication.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
    private final Lep lep = new Lep();
    private final Smpp smpp = new Smpp();
    private final Retry retry = new Retry();
    private int kafkaConcurrencyCount;
    private String kafkaSystemTopic;
    private String kafkaSystemQueue;
    private List<String> tenantIgnoredPathList = Collections.emptyList();
    private boolean streamBindingEnabled;

    private Messaging messaging = new Messaging();

    @Data
    public static class Messaging {
        private String toSendQueueName;
        private String sendFailedQueueName;
        private String sentQueueName;
        private String deliveryFailedQueueName;
        private String deliveredQueueName;
        private Integer deliveryProcessorThreadCount;
        private Integer deliveryMessageQueueMaxSize;
    }

    @Getter
    @Setter
    public static class Lep {
        private TenantScriptStorage tenantScriptStorage;
        private String lepResourcePathPattern;
    }

    @Getter
    @Setter
    public static class Smpp {
        private String host;
        private Long connectionTimeout;
        private Integer port;
        private String systemId;
        private String password;
        private BindType bindType;
        private String systemType;
        private TypeOfNumber addrTon;
        private NumberingPlanIndicator addrNpi;
        private String addressRange;
        private String serviceType;
        private String sourceAddr;
        private TypeOfNumber sourceAddrTon;
        private NumberingPlanIndicator sourceAddrNpi;
        private TypeOfNumber destAddrTon;
        private NumberingPlanIndicator destAddrNpi;
        private int protocolId;
        private int priorityFlag;
        private int replaceIfPresentFlag;
        private int smDefaultMsgId;
        private String validityPeriod;
        private Byte alphaEncoding;
        private Byte cyrillicEncoding;
    }

    @Getter
    @Setter
    private static class Retry {

        private int maxAttempts;
        private long delay;
        private int multiplier;
    }
}
