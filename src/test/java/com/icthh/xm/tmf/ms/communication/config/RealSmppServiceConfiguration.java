package com.icthh.xm.tmf.ms.communication.config;

import com.icthh.xm.tmf.ms.communication.service.DeliveryReportListener;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class RealSmppServiceConfiguration {

    @Bean
    @Primary
    public SmppService realSmppService(ApplicationProperties appProps,
                                       List<DeliveryReportListener> deliveryReportListeners) {
        return new SmppService(appProps, deliveryReportListeners);
    }
}
