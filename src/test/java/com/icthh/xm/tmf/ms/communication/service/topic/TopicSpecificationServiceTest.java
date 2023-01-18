package com.icthh.xm.tmf.ms.communication.service.topic;

import com.icthh.xm.commons.topic.domain.DynamicConsumer;
import com.icthh.xm.tmf.ms.communication.CommunicationApp;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.MessagingConfiguration;
import com.icthh.xm.tmf.ms.communication.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.tmf.ms.communication.mapper.TopicConfigMapper;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.test.binder.MessageCollectorAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = MessageCollectorAutoConfiguration.class)
@SpringBootTest(classes = {CommunicationApp.class, SecurityBeanOverrideConfiguration.class, MessagingConfiguration.class})
public class TopicSpecificationServiceTest {

    private static final String TENANT = "XM";
    private static final String TOPIC_NAME = "communication_XM_queue";

    private TopicSpecificationService subject;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private TopicMessageHandlerFactory topicMessageHandlerFactory;

    @Autowired
    private TopicConfigMapper topicConfigMapper;

    @MockBean
    private SmppService smppService;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        subject = new TopicSpecificationService(applicationProperties, topicConfigMapper, topicMessageHandlerFactory);
    }

    @Test
    public void processTopicSpecification() {
        subject.processTopicSpecifications(TENANT, loadFile("config/specs/topic-spec.yml"));

        List<DynamicConsumer> actual = subject.getDynamicConsumers(TENANT);

        assertThat(actual).isNotEmpty();
        assertThat(actual).hasSize(1);

        DynamicConsumer dynamicConsumer = actual.get(0);

        assertThat(dynamicConsumer.getConfig()).isNotNull();
        assertThat(dynamicConsumer.getConfig().getTopicName()).isEqualTo(TOPIC_NAME);
        assertThat(dynamicConsumer.getConfig().getRetriesCount()).isEqualTo(3);
        assertThat(dynamicConsumer.getConfig().getBackOffPeriod()).isEqualTo(10000);
        assertThat(dynamicConsumer.getConfig().getIsolationLevel()).isEqualTo("read_committed");
        assertThat(dynamicConsumer.getConfig().getDeadLetterQueue()).isEqualTo("test");
        assertThat(dynamicConsumer.getMessageHandler()).isNotNull();
    }

    @Test
    public void processTopicSpecificationWhenConfigEmpty() {
        subject.processTopicSpecifications(TENANT, "");

        List<DynamicConsumer> actual = subject.getDynamicConsumers(TENANT);

        assertThat(actual).isEmpty();
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }
}
