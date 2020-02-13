package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.domain.FirebaseRequest;
import com.icthh.xm.tmf.ms.communication.domain.FirebaseRequestData;
import com.icthh.xm.tmf.ms.communication.utils.ApiMapper;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.rest.errors.FirebaseCommunicatoinException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseService {

    private final RestTemplate restTemplate;

    private final ApplicationProperties applicationProperties;

    public void sendPushNotification(ApiMapper.CommunicationMessageWrapper wrapper) {

        FirebaseRequest request = new FirebaseRequest();
        List<String> tokens = wrapper.getReceivers().stream().map(Receiver::getAppUserId)
            .filter(StringUtils::isNoneBlank).collect(toList());
        request.setRegistrationIds(tokens);


        FirebaseRequestData data = new FirebaseRequestData();
        wrapper.getCharacteristics().forEach(item -> data.addAdditionalData(item.getName(), item.getValue()));
        request.setRequestData(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.set(AUTHORIZATION, applicationProperties.getFirebase().getToken());
        HttpEntity<FirebaseRequest> requestEntity = new HttpEntity<>(request, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(applicationProperties.getFirebase().getUrl(), requestEntity, String.class);
        if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Request to Firebase is failed. status: {}, response: {}", responseEntity.getStatusCode(),
                responseEntity.getBody());
            throw new FirebaseCommunicatoinException(
                String.format("Request to Firebase failed. Status: %s", responseEntity.getStatusCode()));
        }
    }
}
