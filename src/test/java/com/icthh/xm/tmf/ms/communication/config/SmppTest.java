package com.icthh.xm.tmf.ms.communication.config;

import static org.jsmpp.bean.SMSCDeliveryReceipt.SUCCESS_FAILURE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;

@Slf4j
@ContextConfiguration()
@ExtendWith(SpringExtension.class)
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

    public static GenericContainer<?> genericContainer = new GenericContainer<>(DOCKER_IMAGE_NAME)
        .withCreateContainerCmdModifier(getContainerModifier());

    @BeforeAll
    public static void startContainer() {
        genericContainer.start();
    }

    @AfterAll
    public static void stopContainer() {
        genericContainer.stop();
    }

    @Test
    @SneakyThrows
    public void testSendingSms() {
        String messageId = service.send("+380636666666", "test", "1616",
            SUCCESS_FAILURE.value(), Collections.emptyMap(), SmppService.CustomParametersBuilder.builder()
                .validityPeriod(300).protocolId(64).build());
        assertNotNull(messageId);
    }

}
