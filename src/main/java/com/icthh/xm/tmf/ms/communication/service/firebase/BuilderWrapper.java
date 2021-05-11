package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import lombok.Data;

/**
 * Wrapper around Firebase {@link MulticastMessage} builders.
 */
@Data
class BuilderWrapper {
    private MulticastMessage.Builder firebaseMessageBuilder = MulticastMessage.builder();
    private ApnsConfig.Builder apnsBuilder = ApnsConfig.builder();
    private Aps.Builder apsBuilder = Aps.builder();
    private AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder();
    private WebpushConfig.Builder webPushBuilder = WebpushConfig.builder();
    private Notification.Builder notificationBuilder = Notification.builder();
    private AndroidNotification.Builder androidNotificationBuilder = AndroidNotification.builder();
    private WebpushNotification.Builder webpushNotificationBuilder = WebpushNotification.builder();

}
