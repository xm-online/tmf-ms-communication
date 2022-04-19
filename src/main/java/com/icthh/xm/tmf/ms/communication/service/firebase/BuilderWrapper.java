package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import lombok.Value;

/**
 * Wrapper around Firebase {@link MulticastMessage} builders.
 */
@Value
public class BuilderWrapper {
    MulticastMessage.Builder firebaseMessageBuilder = MulticastMessage.builder();
    ApnsConfig.Builder apnsBuilder = ApnsConfig.builder();
    Aps.Builder apsBuilder = Aps.builder();
    AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder();
    WebpushConfig.Builder webPushBuilder = WebpushConfig.builder();
    Notification.Builder notificationBuilder = Notification.builder();
    AndroidNotification.Builder androidNotificationBuilder = AndroidNotification.builder();
    WebpushNotification.Builder webpushNotificationBuilder = WebpushNotification.builder();
}
