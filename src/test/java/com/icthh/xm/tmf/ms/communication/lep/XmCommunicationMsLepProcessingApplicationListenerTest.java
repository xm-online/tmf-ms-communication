package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.lep.core.DefaultScopedContext;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
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

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private XmCommunicationMsLepProcessingApplicationListener listener;

    @Test
    @SuppressWarnings("unchecked")
    public void testBindExecutionContext() {

        ScopedContext context = new DefaultScopedContext("scope");
        listener.bindExecutionContext(context);

        assertEquals(4, context.getValues().size());
        assertNotNull(context.getValue(BINDING_KEY_COMMONS, CommonsExecutor.class));
        assertNotNull(context.getValue(BINDING_KEY_SERVICES, Map.class));
        assertNotNull(context.getValue(BINDING_KEY_TEMPLATES, Map.class));
        assertNotNull(context.getValue(BINDING_KEY_METER_REGISTRY, MeterRegistry.class));

        Map<String, Object> services = (HashMap<String, Object>) context.getValue(BINDING_KEY_SERVICES, HashMap.class);
        assertEquals(3, services.values().size());
        assertNotNull(services.get(BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE));
        assertNotNull(services.get(BINDING_SUB_KEY_PERMISSION_SERVICE));
        assertNotNull(services.get(BINDING_SUB_KEY_SERVICE_MAIL_SERVICE));

        Map<String, Object> templates = (HashMap<String, Object>) context.getValue(BINDING_KEY_TEMPLATES, HashMap.class);
        assertEquals(3, templates.values().size());
        assertNotNull(templates.get(BINDING_SUB_KEY_TEMPLATE_REST));
    }
}
