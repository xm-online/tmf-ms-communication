package com.icthh.xm.tmf.ms.communication.messaging.template;

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessageCreate;

import java.util.Locale;
import java.util.Map;

/**
 * Service to build message content by given template
 */
public interface MessageTemplateService {

    /**
     * Creates message content by given template name and tenant
     * @param tenantKey tenant to fin template spec by
     * @param message communication message to send
     * @return message content
     */
    String getMessageContent(String tenantKey, CommunicationMessageCreate message);

    /**
     * Creates message content by given template name and tenant
     * @param tenantKey tenant to fin template spec by
     * @param templateName specific template name
     * @param objectModel model data to fill up given template with
     * @return message content
     */
    String getMessageContent(String tenantKey, String templateName, Locale locale, Map<String, Object> objectModel);

}
