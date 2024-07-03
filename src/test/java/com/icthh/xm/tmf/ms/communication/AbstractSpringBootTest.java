package com.icthh.xm.tmf.ms.communication;

import lombok.extern.slf4j.Slf4j;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Abstract test for extension for any SpringBoot test.
 *
 * This class prevents Spring test context refreshing between test runs as in case when each Test defines own
 * @SpringBootTest configuration. Marks test with junit @Category
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    TestConfiguration.class,
    CommunicationApp.class,
})
@Category(AbstractSpringBootTest.class)
@Slf4j
public abstract class AbstractSpringBootTest {

}
