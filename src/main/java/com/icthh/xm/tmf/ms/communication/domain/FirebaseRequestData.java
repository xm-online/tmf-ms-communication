package com.icthh.xm.tmf.ms.communication.domain;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@Data
public class FirebaseRequestData {

    private String title;

    private String text;

    private int priority;

    @JsonProperty("content_available")
    private boolean contentAvailable;

    private int visibility;

    private int forceStart;

    private int badge;

    private int notId;

    private boolean showPushPopup;

    private Map<String, Object> additionalData = new HashMap<>();

    public void addAdditionalData(String key, Object value) {
        additionalData.put(key, value);
    }
}
