package com.icthh.xm.tmf.ms.communication.domain;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

@Data
public class FirebaseRequest {

    @JsonProperty("registration_ids")
    private List<String> registrationIds;

    @JsonProperty("data")
    private FirebaseRequestData requestData;
}
