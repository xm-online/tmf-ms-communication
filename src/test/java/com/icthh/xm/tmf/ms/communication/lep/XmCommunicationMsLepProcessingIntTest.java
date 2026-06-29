package com.icthh.xm.tmf.ms.communication.lep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.spring.config.XmAuthenticationContextConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.tmf.ms.communication.LepTestConfiguration;
import com.icthh.xm.tmf.ms.communication.service.lep.DynamicTestLepService;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        LepTestConfiguration.class,
        TenantContextConfiguration.class,
        XmAuthenticationContextConfiguration.class
})
public class XmCommunicationMsLepProcessingIntTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private LepManagementService lepManagementService;

    @Autowired
    private DynamicTestLepService dynamicTestLepService;

    @SneakyThrows
    @BeforeEach
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManagementService.beginThreadContext();
    }

    @AfterEach
    public void tearDown() {
        lepManagementService.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void testLepContextCastToMap() {
        String pathPrefix = "/config/tenants/TEST/testApp/lep/service/";
        String funcKey = pathPrefix + "GetLepContext$$LEP_CONTEXT_TEST$$around.groovy";
        String function = "Map<String, Object> context = lepContext\nreturn ['context':context]";
        leps.onRefresh(funcKey, function);
        Map<String, Object> result = (Map<String, Object>) dynamicTestLepService.getLepContext("LEP_CONTEXT_TEST");
        Object context = result.get("context");
        assertEquals("GroovyMapLepContextWrapper", context.getClass().getSimpleName());
        assertTrue(context instanceof Map);
        leps.onRefresh(funcKey, null);
    }
}
