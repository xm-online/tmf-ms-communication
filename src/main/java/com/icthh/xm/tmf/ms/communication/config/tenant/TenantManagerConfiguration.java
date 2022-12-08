package com.icthh.xm.tmf.ms.communication.config.tenant;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.config.domain.Configuration.of;
import static com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner.prependTenantPath;
import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_EMAILS_PATH_PATTERN;
import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_EMAILS_PATTERN;
import static com.icthh.xm.tmf.ms.communication.config.Constants.DEFAULT_EMAIL_SPEC_CONFIG_PATH;
import static com.icthh.xm.tmf.ms.communication.config.Constants.PATH_TO_EMAILS;
import static com.icthh.xm.tmf.ms.communication.config.Constants.PATH_TO_EMAILS_IN_CONFIG;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.springframework.core.io.support.ResourcePatternUtils.getResourcePatternResolver;

@Slf4j
@org.springframework.context.annotation.Configuration
public class TenantManagerConfiguration {

    private static final String APP_NAME = "communication";
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Bean
    public TenantManager tenantManager(TenantConfigProvisioner configProvisioner) {

        TenantManager manager = TenantManager.builder()
            .service(configProvisioner)
            .build();
        log.info("Configured tenant manager: {}", manager);
        return manager;
    }

    @SneakyThrows
    @Bean
    public TenantConfigProvisioner tenantConfigProvisioner(TenantConfigRepository tenantConfigRepository,
                                                           ApplicationProperties applicationProperties,
                                                           ResourceLoader resourceLoader) {

        Resource[] resources = getResourcePatternResolver(resourceLoader).getResources(DEFAULT_EMAILS_PATTERN);

        List<Configuration> emailConfigs = stream(resources).map(this::resourceToConfiguration).collect(Collectors.toList());

        TenantConfigProvisioner provisioner = TenantConfigProvisioner
            .builder()
            .tenantConfigRepository(tenantConfigRepository)
            .configuration(of().path(applicationProperties.getDefaultEmailSpecificationPathPattern())
                .content(readResource(DEFAULT_EMAIL_SPEC_CONFIG_PATH))
                .build())
            .configurations(emailConfigs)
            .build();

        log.info("Configured tenant config provisioner: {}", provisioner);
        return provisioner;
    }

    @SneakyThrows
    private String readResource(String location) {
        return IOUtils.toString(new ClassPathResource(location).getInputStream(), UTF_8);
    }

    @SneakyThrows
    private Configuration resourceToConfiguration(final Resource resource) {
        String configPath = toFullPath(extractEmailConfigPath(resource));
        String content = IOUtils.toString(resource.getInputStream(), UTF_8);
        return of().path(configPath).content(content).build();
    }

    @SneakyThrows
    private String extractEmailConfigPath(final Resource resource) {
        String path = resource.getURL().getPath();
        int startIndex = path.indexOf(PATH_TO_EMAILS);
        path = path.substring(startIndex);

        String langKey = matcher.extractUriTemplateVariables(DEFAULT_EMAILS_PATH_PATTERN, path).get("langKey");
        String templatePath = matcher.extractPathWithinPattern(DEFAULT_EMAILS_PATH_PATTERN, path);
        String templateFileName = String.format("/%s.ftl", langKey);
        templatePath = templatePath.substring(0, templatePath.lastIndexOf(templateFileName));
        return PATH_TO_EMAILS_IN_CONFIG + templatePath + "/" + langKey + ".ftl";
    }

    private String toFullPath(String path) {
        return prependTenantPath(Paths.get(APP_NAME, path).toString());
    }

}
