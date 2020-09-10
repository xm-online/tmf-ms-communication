package com.icthh.xm.tmf.ms.communication.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Firebase configuration. Uses private key file passed through {@link #FIREBASE_PRIVATE_KEY_NAME}
 * environment variable. Uses Firebase v1 API.
 */
@Configuration
@AllArgsConstructor
public class FirebaseEnvironmentKeyConfiguration {

    public static final String FIREBASE_PRIVATE_KEY_NAME = "FIREBASE_PRIVATE_KEY";

    private final ApplicationProperties applicationProperties;

    @Bean
    @SneakyThrows
    public FirebaseApp firebaseApp() {
        InputStream privateKey =
            new ByteArrayInputStream(Optional.ofNullable(
                System.getenv().get(FIREBASE_PRIVATE_KEY_NAME))
                .filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new IllegalStateException("Environment variable %s is not specified." +
                    "Please securely put Firebase private key json file here."))
                .getBytes());

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(privateKey))
            .setDatabaseUrl(applicationProperties.getFirebase().getUrl())
            .build();

        return FirebaseApp.initializeApp(options);
    }
}
