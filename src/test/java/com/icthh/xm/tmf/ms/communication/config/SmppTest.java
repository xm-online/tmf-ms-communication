package com.icthh.xm.tmf.ms.communication.config;

import static junit.framework.TestCase.assertNotNull;
import static org.jsmpp.bean.SMSCDeliveryReceipt.SUCCESS_FAILURE;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import java.util.Collections;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.GenericContainer;

@Slf4j
@ContextConfiguration()
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {CommunicationApp.class})
public class SmppTest {

    public static final String DOCKER_IMAGE_NAME = "xmonline/smpp-emulator";
    public static final int SMPP_PORT = 2775;

    @Autowired
    private SmppService service;

    private static Consumer<CreateContainerCmd> getContainerModifier() {
        return containerCmd -> containerCmd.withPortBindings(
            new PortBinding(Ports.Binding.bindPort(SMPP_PORT),
            new ExposedPort(SMPP_PORT))
        );
    }

    @ClassRule
    public static GenericContainer<?> genericContainer = new GenericContainer<>(DOCKER_IMAGE_NAME)
        .withCreateContainerCmdModifier(getContainerModifier());

    @Test
    @SneakyThrows
    public void testSendingSms() {
        String messageId = service.send("+380636666666", "test", "1616",
            SUCCESS_FAILURE.value(), Collections.emptyMap(), null);
        assertNotNull(messageId);
    }

}
