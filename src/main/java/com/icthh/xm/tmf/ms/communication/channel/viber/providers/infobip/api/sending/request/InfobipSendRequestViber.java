package com.icthh.xm.tmf.ms.communication.channel.viber.providers.infobip.api.sending.request;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfobipSendRequestViber {
    String text;
    @SerializedName("imageURL")
    String imageUrl;
    String buttonText;
    @SerializedName("buttonURL")
    String buttonUrl;
    Boolean isPromotional;
    Integer validityPeriod;
}
