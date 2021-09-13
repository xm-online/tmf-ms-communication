package com.icthh.xm.tmf.ms.communication.service.smpp;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import java.util.Date;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ConfigurationPropertiesAutoConfiguration.class,
        ApplicationProperties.class,
        ValidityPeriodFactory.class})
@TestPropertySource(locations = "classpath:config/application.yml", properties = {
        "application.smpp.validity-period-type=RELATIVE",
        "application.smpp.validity-period=300"
})
public class ValidityPeriodFactoryTest {


    @Autowired
    private ValidityPeriodFactory validityPeriodFactory;

    @Test
    public void relativeValidityPeriodFormatTest() {
        final String relative = validityPeriodFactory.asString(300, new Date());
        Assert.assertEquals("Smpp relative date format test failed", "000000000500000R", relative);
    }
}