package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.tmf.ms.communication.service.SmppService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class XmCommunicationMsLepProcessingApplicationListenerTest {

    @MockBean
    SmppService smppService;

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private XmCommunicationMsLepContextFactory contextFactory;

    @Test
    @SuppressWarnings("unchecked")
    public void testBindExecutionContext() {

        LepContext context = (LepContext) contextFactory.buildLepContext(null);

        assertNotNull(context.services);
        assertNotNull(context.templates);

        assertNotNull(context.services.meterRegistry);
        assertNotNull(context.services.commonsService);
        assertNotNull(context.services.tenantConfigService);
        assertNotNull(context.services.permissionService);
        assertNotNull(context.services.mailService);

        assertNotNull(context.templates.kafka);
        assertNotNull(context.templates.rest);
        assertNotNull(context.templates.loadBalancedRest);
    }
}
