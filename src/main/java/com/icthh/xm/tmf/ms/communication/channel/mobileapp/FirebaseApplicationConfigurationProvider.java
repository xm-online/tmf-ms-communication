package com.icthh.xm.tmf.ms.communication.channel.mobileapp;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.Optional;

/**
 * Describes a service that works with Firebase Applications
 */
public interface FirebaseApplicationConfigurationProvider {

    /**
     * @return Firebase App for the specified {@code tenantKey} by {@code applicationName}
     */
    FirebaseApp getApplication(String tenantKey, String applicationName);

    /**
     * @return FirebaseMessaging for the specified {@code tenantKey} by {@code applicationName}
     */
    default Optional<FirebaseMessaging> getFirebaseMessaging(String tenantKey, String applicationName) {
        return Optional.ofNullable(getApplication(tenantKey, applicationName))
            .map(FirebaseMessaging::getInstance);
    }
}
