package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.LepConfiguration;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.SmppMessagingHandlerTest.message;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(
    classes = {CommunicationApp.class,
        SecurityBeanOverrideConfiguration.class,
        LepConfiguration.class,})
@Category(CustomCommunicationMessageHandlerTest.class)
@Slf4j
public class CustomCommunicationMessageHandlerTest {

    @Autowired
    private LepManager lepManager;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;
    @Autowired
    CustomCommunicationMessageHandler communicationMessageHandler;
    @Autowired
    BusinessTimeConfigService businessTimeConfigService;
    @Mock
    private XmAuthenticationContextHolder authContextHolder;
    @Mock
    private XmAuthenticationContext context;
    @MockBean
    RestTemplate restTemplate;
    @MockBean
    SmppService smppService;

    private List<String> lepsForCleanUp = new ArrayList<>();


    @Before
    public void before() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        String pattern = "/config/tenants/RESINTTEST/communication/lep/service/message/";
        addLep(pattern, "TEST_VIBER_MESSAGE");
        addLep(pattern, "TEST_TELEGRAM_MESSAGE");
    }

    @After
    public void afterTest() {
        lepsForCleanUp.forEach(it -> leps.onRefresh(it, null));
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    private void addLep(String pattern, String lepName) {
        String lepBody = loadFile("config/testLep/Save$$TEST_MESSAGE_SEND$$around.groovy");
        lepBody = StrSubstitutor.replace(lepBody, of("lepName", lepName));
        leps.onRefresh(pattern + "Send$$" + lepName + "$$around.groovy", lepBody);
        lepsForCleanUp.add(pattern + "Send$$" + lepName + "$$around.groovy");
    }

    @SneakyThrows
    public String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    public void testSuccessLepFindAndExecute() {
        handleMessageWithLepExecute("TEST_VIBER_MESSAGE");
        handleMessageWithLepExecute("TEST_TELEGRAM_MESSAGE");
    }

    @Test
    public void testMessageHandleWithLepNotFind() {
        CommunicationMessage message = message();
        communicationMessageHandler.handle(message);
        assertTrue(message.getCharacteristic().stream()
            .noneMatch(ch -> ch.getName().equals("test") && ch.getValue().equals("ok")));
    }

    private void handleMessageWithLepExecute(String type) {
        CommunicationMessage message = message();
        message.setType(type);
        communicationMessageHandler.handle(message);
        assertTrue(message.getCharacteristic().stream()
            .anyMatch(ch -> ch.getName().equals("test") && ch.getValue().equals("ok")));
    }


}
