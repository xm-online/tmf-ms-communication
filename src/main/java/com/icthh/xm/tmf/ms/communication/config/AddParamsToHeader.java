package com.icthh.xm.tmf.ms.communication.config;


import static com.icthh.xm.commons.tenant.TenantKey.SUPER;

import com.icthh.xm.commons.tenant.XmTenantConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class AddParamsToHeader extends HttpServletRequestWrapper {
    public AddParamsToHeader(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getHeader(String name) {
        if (XmTenantConstants.HTTP_HEADER_TENANT_NAME.equals(name)) {
            return SUPER.getValue();
        }
        String header = super.getHeader(name);
        return (header != null) ? header : super.getParameter(name); // Note: you can't use getParameterValues() here.
    }

    @Override
    public Enumeration getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.addAll(Collections.list(super.getParameterNames()));
        return Collections.enumeration(names);
    }
}
