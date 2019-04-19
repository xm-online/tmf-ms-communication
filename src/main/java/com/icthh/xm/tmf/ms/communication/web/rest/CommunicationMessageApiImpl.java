package com.icthh.xm.tmf.ms.communication.web.rest;

import static com.icthh.xm.tmf.ms.communication.utils.ApiMapper.from;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.tmf.ms.communication.rules.BusinessRuleValidator;
import com.icthh.xm.tmf.ms.communication.rules.RuleResponse;
import com.icthh.xm.tmf.ms.communication.service.SmppService;
import com.icthh.xm.tmf.ms.communication.web.api.CommunicationMessageApiDelegate;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommunicationMessageApiImpl implements CommunicationMessageApiDelegate {

    private final SmppService smppService;
    private final BusinessRuleValidator businessRuleValidator;

    CommunicationMessageApiImpl(SmppService smppService,
        BusinessRuleValidator businessRuleValidator) {
        this.smppService = smppService;
        this.businessRuleValidator = businessRuleValidator;
    }

    @Timed
    public ResponseEntity<CommunicationMessage> createsANewCommunicationMessageAndSendIt(
        CommunicationMessageCreate message) {

        RuleResponse sdsdsd = businessRuleValidator.validate(new CommunicationMessage().id("sdsdsd"));

        log.info( "dsdsds {}", sdsdsd);


        smppService.sendMultipleMessages(from(message).getPhoneNumbers(), message.getContent(),
                                         from(message).getSenderId());
        return ResponseEntity.ok().build();
    }

}
