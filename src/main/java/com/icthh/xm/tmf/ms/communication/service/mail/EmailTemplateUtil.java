package com.icthh.xm.tmf.ms.communication.service.mail;

import com.icthh.xm.commons.tenant.TenantKey;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for working with templates.
 */
@UtilityClass
final class EmailTemplateUtil {

    private static final char SLASH_SIGN = '/';

    /**
     * Email template key generator.
     *
     * @param tenantKey    tenant key
     * @param langKey      localization key
     * @param templateName template file name
     * @return generated key
     */
    static String emailTemplateKey(TenantKey tenantKey, String templateName, String langKey) {
        if (StringUtils.isBlank(langKey) || StringUtils.isBlank(templateName)
            || tenantKey == null
            || StringUtils.isBlank(tenantKey.getValue())) {
            throw new IllegalStateException("Language key, template name and tenant must be not blank");
        }

        return tenantKey.getValue() + SLASH_SIGN + templateName + SLASH_SIGN + langKey;
    }
}
