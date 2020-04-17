package com.icthh.xm.tmf.ms.communication.lep.keresolver;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.upperCase;

@Component
public class CustomMessageResolver extends AppendLepKeyResolver {

    public static final String MESSAGE = "message";

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        CommunicationMessage messageCreate = getRequiredParam(method, MESSAGE, CommunicationMessage.class);
        String translatedLocationTypeKey = upperCase(translateToLepConvention(messageCreate.getType()));
        return new String[] {
            translatedLocationTypeKey
        };
    }
}
