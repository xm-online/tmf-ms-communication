package com.icthh.xm.tmf.ms.communication.lep;

import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.lep.core.DefaultScopedContext;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.tmf.ms.communication.lep.LepXmCommunicationMsConstants.*;
import static org.junit.Assert.*;

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

        assertEquals(3, context.getValues().size());
        assertNotNull(context.getValue(BINDING_KEY_COMMONS, CommonsExecutor.class));
        assertNotNull(context.getValue(BINDING_KEY_SERVICES, Map.class));
        assertNotNull(context.getValue(BINDING_KEY_TEMPLATES, Map.class));

        Map<String, Object> services = (HashMap<String, Object>)context.getValue(BINDING_KEY_SERVICES, HashMap.class);
        assertEquals(3, services.values().size());
        assertNotNull(services.get(BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERICE));
        assertNotNull(services.get(BINDING_SUB_KEY_PERMISSION_SERVICE));
        assertNotNull(services.get(BINDING_SUB_KEY_SERVICE_MAIL_SERVICE));

        Map<String, Object> templates = (HashMap<String, Object>)context.getValue(BINDING_KEY_TEMPLATES, HashMap.class);
        assertEquals(3, templates.values().size());
        assertNotNull(templates.get(BINDING_SUB_KEY_TEMPLATE_REST));
    }
}
