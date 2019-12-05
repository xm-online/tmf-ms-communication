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
        // other channels
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
    @ToString
    public static class Channel {

        private String key;
        private String queue;
    }
}
