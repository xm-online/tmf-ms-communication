package com.icthh.xm.tmf.ms.communication.lep.keresolver;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import org.springframework.stereotype.Component;

@Component
public class CustomMessageCreateResolver extends AppendLepKeyResolver {

    public static final String MESSAGE_CREATE = "messageCreate";

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        CommunicationMessageCreate messageCreate = getRequiredParam(method, MESSAGE_CREATE, CommunicationMessageCreate.class);
        String translatedLocationTypeKey = translateToLepConvention(messageCreate.getType());
        return new String[]{
            translatedLocationTypeKey
        };
    }
}
