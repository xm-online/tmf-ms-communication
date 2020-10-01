package com.icthh.xm.tmf.ms.communication.channel.mobileapp;

import com.google.firebase.FirebaseApp;

/**
 * Describes a service that works with Firebase Applications
 */
public interface FirebaseApplicationConfigurationProvider {

    /**
     * @return Firebase App for the specified {@code tenantKey} by {@code applicationName}
     */
    FirebaseApp getApplication(String tenantKey, String applicationName);
}
