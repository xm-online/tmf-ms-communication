package com.icthh.xm.tmf.ms.communication.messaging.handler;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.LepConfiguration;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.rules.businesstime.BusinessTimeConfigService;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.tmf.ms.communication.messaging.handler.AbstractSmppMessageHandlerUnitTest.message;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(classes = {
    CommunicationApp.class,
    SecurityBeanOverrideConfiguration.class,
    LepConfiguration.class
})
@Slf4j
public class CustomCommunicationMessageHandlerTest {

    @Autowired
    private LepManagementService lepManagementService;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;
    @Autowired
    CustomCommunicationMessageHandler communicationMessageHandler;
    @MockitoBean
    private JavaMailSender javaMailSender;
    @Autowired
    BusinessTimeConfigService businessTimeConfigService;
    @Mock
    private XmAuthenticationContextHolder authContextHolder;
    @Mock
    private XmAuthenticationContext context;
    @MockitoBean
    RestTemplate restTemplate;
    @MockitoBean
    SmppService smppService;

    private List<String> lepsForCleanUp = new ArrayList<>();


    @BeforeEach
    public void before() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        MockitoAnnotations.openMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        lepManagementService.beginThreadContext();

        String pattern = "/config/tenants/RESINTTEST/communication/lep/service/message/";
        addLep(pattern, "TEST_VIBER_MESSAGE");
        addLep(pattern, "TEST_TELEGRAM_MESSAGE");
    }

    @AfterEach
    public void afterTest() {
        lepsForCleanUp.forEach(it -> leps.onRefresh(it, null));
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManagementService.endThreadContext();
    }

    private void addLep(String pattern, String lepName) {
        String lepBody = loadFile("config/testLep/Save$$TEST_MESSAGE_SEND$$around.groovy");
        lepBody = StringSubstitutor.replace(lepBody, of("lepName", lepName));
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
