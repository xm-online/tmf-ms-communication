package com.icthh.xm.tmf.ms.communication.config;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.session.SMPPSession;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static junit.framework.TestCase.assertTrue;

@Slf4j
@ContextConfiguration()
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {CommunicationApp.class})
public class SmppTest {

    public static final String DOCKER_IMAGE_NAME = "smpp-sim";
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
    public static GenericContainer genericContainer = new GenericContainer(DOCKER_IMAGE_NAME)
        .withCreateContainerCmdModifier(getContainerModifier());


    @Test
    public void testIsDockerUp() {
        assertTrue(genericContainer.isRunning());
    }

    @Test
    public void testSendingSms() {
        SMPPSession session = service.getSession();
        service.send(session, "+380636666666", "test");
        session.unbindAndClose();
    }

    @Test
    public void testSendingMultipleMessages() {
        List<String> phones = Arrays.asList("+380636666666", "+380636666665");
        service.sendMultipleMessages(phones, "test");
    }

}
