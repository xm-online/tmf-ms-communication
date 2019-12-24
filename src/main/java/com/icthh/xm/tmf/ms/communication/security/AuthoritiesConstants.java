package com.icthh.xm.tmf.ms.communication.security;

import lombok.experimental.UtilityClass;

/**
 * Constants for Spring Security authorities.
 */
@UtilityClass
public final class AuthoritiesConstants {

    public static final String ADMIN = "SUPER-ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";
}
