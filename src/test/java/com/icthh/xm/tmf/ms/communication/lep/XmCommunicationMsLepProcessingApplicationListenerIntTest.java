package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.tmf.ms.communication.service.SmppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest()
public class XmCommunicationMsLepProcessingApplicationListenerIntTest {

    @MockitoBean
    SmppService smppService;

    @Autowired
    private XmCommunicationMsLepContextFactory contextFactory;

    @Test
    @SuppressWarnings("unchecked")
    public void testBindExecutionContext() {

        LepContext context = (LepContext) contextFactory.buildLepContext(null);

        assertNotNull(context.services);
        assertNotNull(context.templates);

        assertNotNull(context.meterRegistry);
        assertNotNull(context.services.tenantConfigService);
        assertNotNull(context.services.permissionService);
        assertNotNull(context.services.mailService);

        assertNotNull(context.templates.kafka);
        assertNotNull(context.templates.rest);
        assertNotNull(context.templates.loadBalancedRest);
    }
}