package com.icthh.xm.tmf.ms.communication.lep.keresolver;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.tmf.ms.communication.utils.HeaderRequestExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileKeyResolver extends AppendLepKeyResolver {

    private final HeaderRequestExtractor headerRequestExtractor;

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        return new String[]{translateToLepConvention(headerRequestExtractor.getProfile())};
    }

}
