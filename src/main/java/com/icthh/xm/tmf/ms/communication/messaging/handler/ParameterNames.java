package com.icthh.xm.tmf.ms.communication.messaging.handler;

public final class ParameterNames {

    /**
     * Delivery report. Delivery report configuration (byte).
     */
    public static final String DELIVERY_REPORT = "DELIVERY.REPORT";

    /**
     * Optional parameters prefix, e.g. <i>OPTIONAL.6005</i></li>
     */
    public static final String OPTIONAL_PARAMETER_PREFIX = "OPTIONAL.";

    /**
     * Validity period. A number of seconds a message is valid.
     */
    public static final String VALIDITY_PERIOD = "VALIDITY.PERIOD";

    /**
     * Protocol id. SMPP protocol id value.
     */
    public static final String PROTOCOL_ID = "PROTOCOL.ID";

    /**
     * Notification badge. A number that is displayed over the app's icon.
     */
    public static final String BADGE = "BADGE";

    /**
     * URL of the image that is going to be displayed in the notification.
     */
    public static final String IMAGE = "IMAGE";

    /**
     * A unique identifier of the message.
     */
    public static final String MESSAGE_ID = "MESSAGE.ID";

    /**
     * Error code in case of an error.
     */
    public static final String ERROR_CODE = "ERROR.CODE";

    /**
     * Response result type, see {@link ResultType}
     */
    public static final String RESULT_TYPE = "RESULT.TYPE";

    public enum ResultType {

        /**
         * Includes success and error counts only.
         */
        SUMMARY,

        /**
         * Includes success and error counts, detailed description of error cases.
         */
        ERROR,

        /**
         * Includes success and error counts, detailed description
         * of success and errors cases.
         */
        FULL
    }
}
