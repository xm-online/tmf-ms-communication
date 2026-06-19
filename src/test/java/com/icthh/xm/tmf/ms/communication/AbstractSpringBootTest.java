package com.icthh.xm.tmf.ms.communication;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Abstract test for extension for any SpringBoot test.
 *
 * This class prevents Spring test context refreshing between test runs as in case when each Test defines own
 * @SpringBootTest configuration. Marks test with junit @Category
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    TestConfiguration.class,
    CommunicationApp.class,
})
@Tag("com.icthh.xm.tmf.ms.communication.AbstractSpringBootTest")
@Slf4j
public abstract class AbstractSpringBootTest {

}
