package com.icthh.xm.tmf.ms.communication.lep.keresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomMessageCreateResolver implements LepKeyResolver {

    public static final String MESSAGE_CREATE = "messageCreate";

    @Override
    public List<String> segments(LepMethod method) {
        CommunicationMessageCreate messageCreate = method.getParameter(MESSAGE_CREATE, CommunicationMessageCreate.class);
        return List.of(messageCreate.getType());
    }
}
