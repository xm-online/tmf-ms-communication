package com.icthh.xm.tmf.ms.communication.web.rest;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.metric.MetricsConfiguration;
import com.icthh.xm.tmf.ms.communication.channel.telegram.TelegramChannelHandler;
import com.icthh.xm.tmf.ms.communication.channel.twilio.TwilioChannelHandler;
import com.icthh.xm.tmf.ms.communication.config.ChannelRefreshableConfiguration;
import com.icthh.xm.tmf.ms.communication.security.AuthoritiesConstants;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class RetrieveCommunicationMessageTest {
    @LocalServerPort
    public Integer localServicePort;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private MetricRegistry metricRegistry;
    @MockBean
    private MetricsConfiguration metricsConfiguration;
    @MockBean
    private TelegramChannelHandler telegramChannelHandler;
    @MockBean
    private TwilioChannelHandler twilioChannelHandler;
    @MockBean
    private ChannelRefreshableConfiguration channelRefreshableConfiguration;
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private SmppService smppService;
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private KafkaTemplate<String, String> template;

    @Autowired
    private ResourceServerTokenServices tokenServices;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testRetrieveCommunicationMessage() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-tenant", "XM");
        headers.add("Authorization", createAccessToken("admin"));
        String baseURL = "http://127.0.0.1:" + localServicePort;
        String url = baseURL + "/api/communicationManagement/v2/communicationMessage/TRANSFER.SUCCESS?language=en";

        ResponseEntity<List<CommunicationMessage>> responseEntity =
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), messageTypeReference());
        CommunicationMessage message = responseEntity.getBody().get(0);

        assertEquals("en", message.getContent());
        assertEquals("TRANSFER.SUCCESS", message.getId());
    }

    private ParameterizedTypeReference<List<CommunicationMessage>> messageTypeReference() {
        return new ParameterizedTypeReference<>() {
        };
    }

    private String createAccessToken(String username) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(username, "", AuthoritiesConstants.ADMIN);
        OAuth2Request authRequest = new OAuth2Request(null, "", token.getAuthorities(), true, null, null, null, null, null);
        OAuth2Authentication oauth = new OAuth2Authentication(authRequest, token);
        oauth.setDetails(ImmutableMap.of("tenant", "XM"));
        String accessToken = UUID.randomUUID().toString();
        Mockito.when(tokenServices.loadAuthentication(accessToken)).thenReturn(oauth);
        return "Bearer " + accessToken;
    }
}
