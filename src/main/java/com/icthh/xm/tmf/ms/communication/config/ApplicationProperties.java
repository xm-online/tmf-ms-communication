package com.icthh.xm.tmf.ms.communication.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * Properties specific to Communication.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private List<String> tenantIgnoredPathList = Collections.emptyList();

    private LepProperties lep;

    @Data
    public static class LepProperties {
        private String tenantScriptStorage;
    }

}
