package com.icthh.xm.tmf.ms.communication.service.firebase;

import com.google.firebase.messaging.*;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Configurator for Firebase builders that allows implement custom logic via LEPs.
 */
@Slf4j
@LepService(group = "service.message.firebase")
public class BuilderConfigurator {
    @LogicExtensionPoint(value = "CustomizeApnsConfig", resolver = CustomMessageResolver.class)
    public ApnsConfig getApnsConfig(final BuilderWrapper builderWrapper, CommunicationMessage message) {
        Aps aps = builderWrapper.getApsBuilder().build();
        return builderWrapper.getApnsBuilder()
            .setAps(aps)
            .build();
    }

    @LogicExtensionPoint(value = "CustomizeWebpushConfig", resolver = CustomMessageResolver.class)
    public WebpushConfig getWebpushConfig(final BuilderWrapper builderWrapper, CommunicationMessage message) {
        WebpushNotification webpushNotification = builderWrapper.getWebpushNotificationBuilder().build();
        return builderWrapper.getWebPushBuilder()
                .setNotification(webpushNotification)
                .build();
    }

    @LogicExtensionPoint(value = "CustomizeAndroidConfig", resolver = CustomMessageResolver.class)
    public AndroidConfig getAndroidConfig(final BuilderWrapper builderWrapper, CommunicationMessage message) {
        AndroidNotification androidNotification = builderWrapper.getAndroidNotificationBuilder().build();
        return builderWrapper.getAndroidConfigBuilder()
                .setNotification(androidNotification)
                .build();
    }

    @LogicExtensionPoint(value = "CustomizeNotification", resolver = CustomMessageResolver.class)
    public Notification getNotification(final BuilderWrapper builderWrapper, CommunicationMessage message) {
        return builderWrapper.getNotificationBuilder().build();
    }
}
