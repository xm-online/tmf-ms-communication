package com.icthh.xm.tmf.ms.communication.service.lep;

import com.icthh.xm.commons.lep.LogicExtensionPoint;

import com.icthh.xm.commons.lep.spring.LepService;


@LepService(group = "service")
public class DynamicTestLepService {

    @LogicExtensionPoint(value = "GetLepContext", resolverExpression = "#input")
    public Object getLepContext(String input) {
        return null;
    }
}
