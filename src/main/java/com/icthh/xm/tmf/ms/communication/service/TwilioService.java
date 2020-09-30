package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.tmf.ms.communication.domain.CommunicationSpec;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import com.icthh.xm.tmf.ms.communication.web.api.model.Receiver;
import com.icthh.xm.tmf.ms.communication.web.api.model.Sender;
import com.twilio.http.TwilioRestClient;
import com.twilio.http.ValidationClient;
import com.twilio.rest.accounts.v1.credential.PublicKey;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.rest.api.v2010.account.NewSigningKey;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import javax.xml.bind.DatatypeConverter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.icthh.xm.tmf.ms.communication.messaging.handler.CommunicationMessageMapper.INSTANCE;

@Slf4j
@Service
@IgnoreLogginAspect
@RequiredArgsConstructor
public class TwilioService  implements MessageService {

    private Map<String, Map<String, TwilioRestClient>> twilioClients = new ConcurrentHashMap<>();

    @Override
    public CommunicationMessage recive(String tenantKey, CommunicationMessage message) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public CommunicationMessage send(String tenantKey, CommunicationMessageCreate message) {
        String senderKey = message.getSender().getId();
        Map<String, TwilioRestClient> clients = getClientsByTenant(tenantKey);
        TwilioRestClient client = clients.get(senderKey);

        if (client == null) {
            log.warn("Twilio sender not found for key: [{}]. Message: [{}] skipped.", senderKey, message.getContent());
            return new CommunicationMessage();
        }

        if (message.getReceiver().size() != 1) {
            throw new BusinessException("Only one receiver should be provided. Current number=" + message.getReceiver().size());
        }

        String senderPhoneNumber = message.getSender().getPhoneNumber();
        if (StringUtils.isEmpty(senderPhoneNumber)) {
            throw new BusinessException("Sender PhoneNumber is not provided");
        }

        Message createdMessage = byPhoneNumber(message.getReceiver().get(0), senderPhoneNumber, message.getContent()).create(client);
        log.debug("twilioResponse: {}", createdMessage);
        CommunicationMessage result = INSTANCE.messageCreateToMessage(message);
        result.setSendTime(OffsetDateTime.now());
        result.setSendTimeComplete(OffsetDateTime.now());
        result.setStatus(createdMessage.getStatus().toString());
        result.setHref(createdMessage.getUri());

        return result;
    }

    public void registerSender(String tenantKey, CommunicationSpec.Twilio twilioConfig) {
        Map<String, TwilioRestClient> clients = getClientsByTenant(tenantKey);
        if (clients.containsKey(twilioConfig.getKey())) {
            log.info("[{}] Skip twilio registration because such sender already registered: [{}]",
                tenantKey, twilioConfig);
            return;
        }
        withLog("twilioSender", () -> startTwilioSender(tenantKey, twilioConfig), twilioConfig.getKey());
    }

    public void unregisterTwilioSenders(String tenantKey) {
        twilioClients.remove(tenantKey);
    }

    protected MessageCreator byNumberOrSid(Receiver receiver, CommunicationMessageCreate message, String serviceId) {
        return Optional.ofNullable(message.getSender())
            .map(Sender::getPhoneNumber)
            .filter(StringUtils::isNotEmpty)
            .map(sender -> byPhoneNumber(receiver, sender, message.getContent()))
            .orElseGet(() -> byAccountSid(receiver, serviceId, message.getContent()));
    }

    private MessageCreator byPhoneNumber(Receiver receiver, String sender, String message) {
        return Message.creator(
            new PhoneNumber(receiver.getPhoneNumber()),
            new PhoneNumber(sender),
            message);
    }

    private MessageCreator byAccountSid(Receiver receiver, String accountSid, String message) {
        return Message.creator(
            new PhoneNumber(receiver.getPhoneNumber()),
            accountSid,
            message);
    }

    private Map<String, TwilioRestClient> getClientsByTenant(String tenantKey) {
        return twilioClients.getOrDefault(tenantKey, new ConcurrentHashMap<>());
    }

    @SneakyThrows
    private void startTwilioSender(String tenantKey, CommunicationSpec.Twilio twilioConfig) {
        // Use the default rest client
        TwilioRestClient client =
            new TwilioRestClient.Builder(twilioConfig.getAccountSid(), twilioConfig.getAuthToken())
                .build();

        if (Boolean.TRUE.equals(twilioConfig.getUseClientValidationFeature())) {
            client = validationClient(client, twilioConfig);
        }

        Map<String, TwilioRestClient> clients = twilioClients.getOrDefault(tenantKey, new ConcurrentHashMap<>());
        clients.put(twilioConfig.getKey(), client);
        twilioClients.put(tenantKey, clients);
    }

    @SneakyThrows
    private TwilioRestClient validationClient(TwilioRestClient client, CommunicationSpec.Twilio twilioConfig) {
        // Generate public/private key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        java.security.PublicKey pk = pair.getPublic();
        // Create a public key and signing key using the default client
        PublicKey key = PublicKey.creator(
            DatatypeConverter.printBase64Binary(pk.getEncoded())
        ).setFriendlyName("Public Key").create(client);

        NewSigningKey signingKey = NewSigningKey.creator().create(client);

        // Switch to validation client as the default client
        return new TwilioRestClient.Builder(signingKey.getSid(), signingKey.getSecret())
            .accountSid(twilioConfig.getAccountSid())
            .httpClient(new ValidationClient(twilioConfig.getAccountSid(), key.getSid(), signingKey.getSid(), pair.getPrivate()))
            .build();
    }

    private void withLog(String command, Runnable action, Object... params) {
        final StopWatch stopWatch = StopWatch.createStarted();
        log.info("start: {} {}", command, params);
        action.run();
        log.info(" stop: {}, time = {} ms.", command, stopWatch.getTime());
    }

}
