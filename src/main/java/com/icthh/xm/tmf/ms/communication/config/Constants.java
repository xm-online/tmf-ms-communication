package com.icthh.xm.tmf.ms.communication.config;

/**
 * Application constants.
 */
public final class Constants {

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_.@A-Za-z0-9-]*$";

    public static final String SYSTEM_ACCOUNT = "system";
    public static final String ANONYMOUS_USER = "anonymoususer";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String TRANSLATION_KEY = "trKey";
    public static final String DEFAULT_EMAIL_SPEC_CONFIG_PATH = "config/specs/default-email-spec.yml";
    public static final String DEFAULT_EMAILS_PATTERN = "classpath*:config/emails/**/*.ftl";
    public static final String PATH_TO_EMAILS = "/config/emails/";
    public static final String DEFAULT_EMAILS_PATH_PATTERN = "/config/emails/**/{langKey}.ftl";
    public static final String PATH_TO_EMAILS_IN_CONFIG = "/emails/";

    public static final String CONFIG_PATH_TEMPLATE = "/config/tenants/%s/communication/";
    public static final String CUSTOM_EMAIL_SPEC = "custom-email-spec.yml";
    public static final String CUSTOM_EMAIL_PATH = "custom-emails/";

    private Constants() {
    }
}
