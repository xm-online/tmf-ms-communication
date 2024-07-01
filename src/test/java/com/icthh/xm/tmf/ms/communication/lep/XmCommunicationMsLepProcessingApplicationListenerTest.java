package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.GroovyMapLepContextWrapper;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_KEY_COMMONS;
import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_KEY_METER_REGISTRY;
import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_KEY_SERVICES;
import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_KEY_TEMPLATES;
import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_SUB_KEY_PERMISSION_SERVICE;
import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_SUB_KEY_SERVICE_MAIL_SERVICE;
import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE;
import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.BINDING_SUB_KEY_TEMPLATE_REST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class XmCommunicationMsLepProcessingApplicationListenerTest {

    @MockBean
    SmppService smppService;

    @Autowired
    private XmCommunicationMsLepContextFactory contextFactory;

    @Test
    @SuppressWarnings("unchecked")
    public void testBindExecutionContext() {

        LepContext context = (LepContext) contextFactory.buildLepContext(null);

        assertNotNull(context.services);
        assertNotNull(context.templates);
        assertNotNull(context.commons);

        assertNotNull(context.meterRegistry);
        assertNotNull(context.services.tenantConfigService);
        assertNotNull(context.services.permissionService);
        assertNotNull(context.services.mailService);

        assertNotNull(context.templates.kafka);
        assertNotNull(context.templates.rest);
        assertNotNull(context.templates.loadBalancedRest);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindExecutionContextMap() {

        GroovyMapLepContextWrapper context = (GroovyMapLepContextWrapper) contextFactory.buildLepContext(null);

        assertEquals(4, context.size());
        assertNotNull(context.get(BINDING_KEY_COMMONS));
        assertNotNull(context.get(BINDING_KEY_SERVICES));
        assertNotNull(context.get(BINDING_KEY_TEMPLATES));
        assertNotNull(context.get(BINDING_KEY_METER_REGISTRY));

        Map<String, Object> services = (HashMap<String, Object>) context.get(BINDING_KEY_SERVICES);
        assertEquals(3, services.values().size());
        assertNotNull(services.get(BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE));
        assertNotNull(services.get(BINDING_SUB_KEY_PERMISSION_SERVICE));
        assertNotNull(services.get(BINDING_SUB_KEY_SERVICE_MAIL_SERVICE));

        Map<String, Object> templates = (HashMap<String, Object>) context.get(BINDING_KEY_TEMPLATES);
        assertEquals(3, templates.values().size());
        assertNotNull(templates.get(BINDING_SUB_KEY_TEMPLATE_REST));
    }

}
