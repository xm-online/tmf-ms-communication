package com.icthh.xm.tmf.ms.communication.lep.keresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.upperCase;

@Component
public class CustomMessageResolver implements LepKeyResolver {

    public static final String MESSAGE = "message";

    @Override
    public List<String> segments(LepMethod method) {
        CommunicationMessage message = method.getParameter(MESSAGE, CommunicationMessage.class);
        return List.of(upperCase(message.getType()));
    }
}
