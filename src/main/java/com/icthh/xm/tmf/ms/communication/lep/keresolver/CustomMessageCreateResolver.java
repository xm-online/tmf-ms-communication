package com.icthh.xm.tmf.ms.communication.lep.keresolver;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;
import org.springframework.stereotype.Component;

import static com.icthh.xm.tmf.ms.communication.lep.keresolver.CustomMessageResolver.MESSAGE;

@Component
public class CustomMessageCreateResolver extends AppendLepKeyResolver {


    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        CommunicationMessageCreate messageCreate = getRequiredParam(method, MESSAGE, CommunicationMessageCreate.class);
        String translatedLocationTypeKey = translateToLepConvention(messageCreate.getType());
        return new String[] {
            translatedLocationTypeKey
        };
    }
}
