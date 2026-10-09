package com.icthh.xm.tmf.ms.communication.service.mail;

import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;

/**
 * Restricts a shared {@link TemplateLoader}, whose template names are {@code TENANT/path/lang},
 * to the templates of a single tenant. Prevents {@code <#include>} and {@code <#import>}
 * from reading templates of another tenant.
 */
public class TenantScopedTemplateLoader implements TemplateLoader {

    private final String tenantPrefix;
    private final TemplateLoader delegate;

    public TenantScopedTemplateLoader(String tenantKey, TemplateLoader delegate) {
        this.tenantPrefix = tenantKey + "/";
        this.delegate = delegate;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return name.startsWith(tenantPrefix) ? delegate.findTemplateSource(name) : null;
    }

    @Override
    public long getLastModified(Object templateSource) {
        return delegate.getLastModified(templateSource);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        return delegate.getReader(templateSource, encoding);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        delegate.closeTemplateSource(templateSource);
    }
}
