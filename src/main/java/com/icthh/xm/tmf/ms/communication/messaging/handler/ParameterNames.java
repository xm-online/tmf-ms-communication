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

    /**
     * Override source type TON
     */
    public static final String SOURCE_TYPE_TON = "SOURCE.TON";

    /**
     * Override destination type TON
     */
    public static final String DESTINATION_TYPE_TON = "DESTINATION.TON";

    /**
     * WAP Push hex-encoded payload (full OMA/WSP binary payload as a hex string).
     */
    public static final String WAP_PUSH_HEX_PAYLOAD = "WAP.PUSH.HEX.PAYLOAD";

    /**
     * WAP Push segment hex strings, comma-separated, for debugging/logging purposes.
     */
    public static final String WAP_PUSH_SEGMENTS_HEX = "WAP.PUSH.SEGMENTS.HEX";

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
