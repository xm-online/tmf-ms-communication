package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.tmf.ms.communication.config.XmFreeMarkerConfiguration.XmFreeMarkerConfigurer;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that the FreeMarker configuration used by every renderer in the service is sandboxed:
 * template authors must not be able to instantiate classes or reach the Java API via ?new, ?interpret or ?api.
 */
public class XmFreeMarkerConfigurationUnitTest {

    private static final String EXECUTE_VIA_NEW =
        "<#assign ex = \"freemarker.template.utility.Execute\"?new()>${ex(\"id\")}";

    private Configuration configuration;

    @BeforeEach
    public void setUp() throws Exception {
        XmFreeMarkerConfigurer configurer = new XmFreeMarkerConfigurer(new StringTemplateLoader());
        configurer.afterPropertiesSet();
        configuration = new XmFreeMarkerConfiguration().freeMarkerConfiguration(configurer);
    }

    @Test
    public void rejectsClassInstantiationViaNewBuiltin() {
        assertThatThrownBy(() -> render(EXECUTE_VIA_NEW, Map.of()))
            .isInstanceOf(TemplateException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    public void rejectsClassInstantiationInsideInterpret() {
        String content = "<@r\"" + EXECUTE_VIA_NEW.replace('"', '\'') + "\"?interpret />";

        assertThatThrownBy(() -> render(content, Map.of()))
            .isInstanceOf(TemplateException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    public void rejectsApiBuiltin() {
        assertThatThrownBy(() -> render("${model?api.getClass()}", Map.of("model", Map.of())))
            .isInstanceOf(TemplateException.class)
            .hasMessageContaining("api_builtin_enabled");
    }

    @Test
    public void rendersExpressionsAndNestedModelValues() throws Exception {
        String rendered = render("FM_RENDER_CHECK_${7 * 7} ${user.firstName}",
            Map.of("user", Map.of("firstName", "Name")));

        assertThat(rendered).isEqualTo("FM_RENDER_CHECK_49 Name");
    }

    private String render(String content, Map<String, Object> model) throws Exception {
        Template template = new Template("test", content, configuration);
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
    }
}
