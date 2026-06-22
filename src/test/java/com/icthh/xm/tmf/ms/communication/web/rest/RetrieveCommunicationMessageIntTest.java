package com.icthh.xm.tmf.ms.communication.web.rest;

import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.commons.security.internal.XmAuthenticationDetails;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.tmf.ms.communication.channel.telegram.TelegramChannelHandler;
import com.icthh.xm.tmf.ms.communication.channel.twilio.TwilioChannelHandler;
import com.icthh.xm.tmf.ms.communication.config.ChannelRefreshableConfiguration;
import com.icthh.xm.tmf.ms.communication.security.AuthoritiesConstants;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class RetrieveCommunicationMessageIntTest {
    @LocalServerPort
    public Integer localServicePort;

    @MockitoBean
    private TelegramChannelHandler telegramChannelHandler;
    @MockitoBean
    private TwilioChannelHandler twilioChannelHandler;
    @MockitoBean
    private JavaMailSender javaMailSender;
    @MockitoBean
    private ChannelRefreshableConfiguration channelRefreshableConfiguration;
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private SmppService smppService;
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private KafkaTemplate<String, String> template;

    @MockitoBean
    private TokenProvider tokenProvider;

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
        String accessToken = UUID.randomUUID().toString();
        Claims claims = mock(Claims.class);
        XmAuthenticationDetails details = mock(XmAuthenticationDetails.class);
        XmAuthentication authentication = new XmAuthentication(details, username,
            List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)));
        authentication.setAuthenticated(true);

        when(tokenProvider.validateToken(accessToken)).thenReturn(claims);
        when(tokenProvider.getAuthentication((HttpServletRequest) any(), eq(accessToken), eq(claims))).thenReturn(authentication);
        return "Bearer " + accessToken;
    }
}
