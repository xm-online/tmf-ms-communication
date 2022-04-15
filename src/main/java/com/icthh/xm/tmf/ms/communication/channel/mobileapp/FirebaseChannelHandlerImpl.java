package com.icthh.xm.tmf.ms.communication.channel.mobileapp;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.icthh.xm.tmf.ms.communication.channel.ChannelHandler;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementation that works with Firebase/MobileApp channel.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "application.firebase.enabled", havingValue = "true")
@AllArgsConstructor
public class FirebaseChannelHandlerImpl implements FirebaseApplicationConfigurationProvider,
    ChannelHandler {

    private static final Map<String, Map<String, FirebaseApp>> tenantApps = new HashMap<>();
    private final ApplicationProperties applicationProperties;

    @Override
    public FirebaseApp getApplication(String tenantKey, String applicationName) {
        return Optional.ofNullable(tenantApps.get(tenantKey))
            .map(apps -> apps.get(buildAppName(tenantKey, applicationName)))
            .orElse(null);
    }

    @Override
    public void onRefresh(String tenantKey, CommunicationSpec spec) {
        if (spec == null || spec.getChannels() == null
            || spec.getChannels().getMobileApp() == null) {
            log.warn("Channel config is not specified although Firebase is enabled");
            return;
        }

        for (CommunicationSpec.Firebase config : spec.getChannels().getMobileApp()) {
            configure(tenantKey, config);
        }
    }

    /**
     * Configures a Firebase application. Uses private key file passed through
     * {@link CommunicationSpec.Firebase#getPrivateKeyEnvironmentVariableName()}
     * environment variable. Uses Firebase v1 API.
     */
    @SneakyThrows
    private void configure(String tenantKey, CommunicationSpec.Firebase config) {
        String keyName = config.getPrivateKeyEnvironmentVariableName();

        InputStream privateKey =
            new ByteArrayInputStream(Optional.ofNullable(
                System.getenv().get(keyName))
                .filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new IllegalStateException(String.format("Environment variable %s is not specified." +
                    "Please securely put Firebase private key json file here.", keyName)))
                .getBytes());

        FirebaseOptions options = buildFirebaseOptions(config, privateKey);

        String appName = buildAppName(tenantKey, config.getApplicationName());

        Map<String, FirebaseApp> apps = tenantApps.computeIfAbsent(tenantKey, t -> new HashMap<>());

        FirebaseApp existing = apps.get(appName);

        if (existing != null) {
            log.info("Deleting a Firebase application {}", appName);
            existing.delete();
        }

        log.info("Initializing Firebase application {}", appName);
        FirebaseApp firebaseApp = FirebaseApp.initializeApp(options, appName);
        apps.put(appName, firebaseApp);
    }

    private FirebaseOptions buildFirebaseOptions(CommunicationSpec.Firebase config, InputStream privateKey) throws IOException {

        FirebaseOptions.Builder builder = FirebaseOptions.builder();
        NetHttpTransport.Builder httpTransportBuilder = new NetHttpTransport.Builder();

        Optional.ofNullable(applicationProperties.getFirebase())
            .map(ApplicationProperties.Firebase::getProxy)
            .filter(p -> StringUtils.isNoneBlank(p.getHost()))
            .filter(p -> StringUtils.isNoneBlank(p.getPort()))
            .ifPresent(proxySettings -> {
                log.info("Using proxy settings {}", proxySettings);
                httpTransportBuilder.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                    proxySettings.getHost(), Integer.parseInt(proxySettings.getPort()))));
            });

        NetHttpTransport httpTransport = httpTransportBuilder.build();
        builder.setHttpTransport(httpTransport)
            .setCredentials(GoogleCredentials.fromStream(privateKey, () -> httpTransport))
            .setDatabaseUrl(config.getDatabaseUrl());

        return builder.build();
    }

    private String buildAppName(String tenantKey, String appName) {
        return String.format("%s:%s", tenantKey, appName);
    }
}
