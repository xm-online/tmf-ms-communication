package com.icthh.xm.tmf.ms.communication.lep.fields;

import com.icthh.xm.commons.lep.api.LepAdditionalContextField;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;

public interface CommonsField extends LepAdditionalContextField {
    String FIELD_NAME = "commonsService";
    default CommonsExecutor getCommonsService() {
        return (CommonsExecutor) get(FIELD_NAME);
    }
}
