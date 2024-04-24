package com.icthh.xm.tmf.ms.communication.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Specification for the configuration of channels
 */
@Data
public class CommunicationSpec implements Serializable {

    private Channels channels;

    @Getter
    @Setter
    @ToString
    public static class Channels {

        private List<Telegram> telegram = new LinkedList<>();
        private List<Viber> viber = new LinkedList<>();
        private List<Sms> sms = new LinkedList<>();
        private List<Twilio> twilio = new LinkedList<>();
        private List<Firebase> mobileApp = new LinkedList<>();
        // other channels
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class Twilio extends Channel {
        /**
         * This is enterprise level feature. Details https://www.twilio.com/docs/iam/pkcv
         */
        private Boolean useClientValidationFeature;
        private String accountSid;
        private String authToken;
        private String defaultSender;
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class Telegram extends Channel {

        private String token;
    }


    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class Viber extends Channel {

        private String appKey;
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class Sms extends Channel {

        private Map<String, String> prop;
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class Firebase extends Channel {
        private String applicationName;
        private String privateKeyEnvironmentVariableName;
        private String databaseUrl;
        private Map<String, String> prop;
    }

    @Getter
    @Setter
    @ToString
    public static class Channel {

        private String key;
        private String queue;
    }
}
