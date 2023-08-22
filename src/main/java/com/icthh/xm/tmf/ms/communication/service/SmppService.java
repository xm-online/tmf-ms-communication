package com.icthh.xm.tmf.ms.communication.service;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.jsmpp.bean.Alphabet.ALPHA_DEFAULT;
import static org.jsmpp.bean.Alphabet.ALPHA_UCS2;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_PAYLOAD;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Smpp;
import com.icthh.xm.tmf.ms.communication.messaging.MessageReceiverListenerAdapter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.IntegerValidator;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmppService {

    private static final byte[] EMPTY_MESSAGE = "".getBytes();

    private final AbsoluteTimeFormatter timeFormatter = new AbsoluteTimeFormatter();
    private final ApplicationProperties appProps;
    private final List<DeliveryReportListener> deliveryReportListeners;

    private volatile SMPPSession session;

    @SneakyThrows
    private SMPPSession createSession(ApplicationProperties appProps) {
        SMPPSession session = new SMPPSession();
        Smpp smpp = appProps.getSmpp();
        BindParameter bindParam = new BindParameter(
            smpp.getBindType(),
            smpp.getSystemId(),
            smpp.getPassword(),
            smpp.getSystemType(),
            smpp.getAddrTon(),
            smpp.getAddrNpi(),
            smpp.getAddressRange()
        );
        session.setTransactionTimer(smpp.getConnectionTimeout());
        session.connectAndBind(smpp.getHost(), smpp.getPort(), bindParam);
        session.setMessageReceiverListener((MessageReceiverListenerAdapter) (deliverySm) -> {
            try {
                MdcUtils.putRid(MdcUtils.generateRid());
                deliveryReportListeners.forEach(it -> it.onAcceptDeliverSm(deliverySm));
            } catch (Exception e) {
                log.error("Error process delivery report", e);
            } finally {
                MdcUtils.removeRid();
            }
        });
        session.addSessionStateListener((newState, oldState, source) -> {
            if (!newState.isBound()) {
                getActualSession();
            }
        });
        return session;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (appProps.getSmpp().getEnabled()) {
            getActualSession();
        }
    }

    /**
     * Send a message
     *
     * @param destAddrs          destination number
     * @param message            text message
     * @param senderId           sender number or alpha-name
     * @param optionalParameters optional parameters, key - parameter tag, value - parameter value
     * @param validityPeriod     a number of seconds a message is valid.
     *                           If is not delivered in this period will get EXPIRED delivery state
     *                           (uses the default if null).
     * @param protocolId         smpp protocol id (uses the default if null)
     * @return message id - unique identifier of the message. Used in delivery reports.
     */
    @Deprecated(since = "Since method send with customParameters was introduced")
    public String send(String destAddrs, String message, String senderId, byte deliveryReport, Map<Short,
        String> optionalParameters, Integer validityPeriod, Integer protocolId) throws PDUException, IOException,
        InvalidResponseException,
        NegativeResponseException,
        ResponseTimeoutException {

        Smpp smpp = appProps.getSmpp();

        DataCoding dataCoding = getDataCoding(message);
        log.info("Start send message from: {} to: {} with encoding [{}] and content.size: {}, " +
                "optional parameters: {}, validity period: {}, protocol id: {}", senderId, destAddrs,
            dataCoding, message.length(), optionalParameters, validityPeriod, protocolId);

        List<OctetString> optional = optionalParameters.entrySet().stream()
            .map(e -> new OctetString(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        optional.add(toPayload(message));

        SMPPSession session = getActualSession();

        Date scheduleDeliveryTime = new Date();
        String messageId = session.submitShortMessage(
            smpp.getServiceType(),
            smpp.getSourceAddrTon(),
            smpp.getSourceAddrNpi(),
            getSourceAddr(senderId, smpp),
            smpp.getDestAddrTon(),
            smpp.getDestAddrNpi(),
            destAddrs,
            new ESMClass(),
            (protocolId != null ? protocolId.byteValue() : (byte) smpp.getProtocolId()),
            (byte) smpp.getPriorityFlag(),
            timeFormatter.format(scheduleDeliveryTime),
            validityPeriodOrDefault(validityPeriod, scheduleDeliveryTime,
                IntegerValidator.getInstance().validate(smpp.getValidityPeriod())),
            new RegisteredDelivery(deliveryReport),
            (byte) smpp.getReplaceIfPresentFlag(), dataCoding,
            (byte) smpp.getSmDefaultMsgId(),
            EMPTY_MESSAGE,
            optional.toArray(OctetString[]::new)
        );
        log.info("Message submitted, message_id is {}", messageId);
        return messageId;
    }

    /**
     * Send a message
     *
     * @param destAddrs          destination number
     * @param message            text message
     * @param senderId           sender number or alpha-name
     * @param deliveryReport     enabled delivery report
     * @param optionalParameters optional parameters, key - parameter tag, value - parameter value
     * @param customParameters   custom parameters. {@link com.icthh.xm.tmf.ms.communication.service.SmppService.CustomParametersBuilder}
     *
     * @return message id - unique identifier of the message. Used in delivery reports.
     */
    public String send(String destAddrs, String message, String senderId, byte deliveryReport, Map<Short,
        String> optionalParameters, CustomParametersBuilder customParameters) throws PDUException, IOException,
        InvalidResponseException,
        NegativeResponseException,
        ResponseTimeoutException {

        DataCoding dataCoding = getDataCoding(message);
        log.info("Start send message from: {} to: {} with encoding [{}] and content.size: {}, " +
                "optional parameters: {}, custom parameters: {}", senderId, destAddrs,
            dataCoding, message.length(), optionalParameters, customParameters);

        List<OctetString> optional = optionalParameters.entrySet().stream()
            .map(e -> new OctetString(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        optional.add(toPayload(message));

        SmppShortMessageConfigHolder shortMessageBuilder =
            buildMessageParameters(senderId, destAddrs, deliveryReport, customParameters, dataCoding, optional);

        SMPPSession session = getActualSession();
        String messageId = session.submitShortMessage(
            shortMessageBuilder.getServiceType(),
            shortMessageBuilder.getSourceAddrTon(),
            shortMessageBuilder.getSourceAddrNpi(),
            shortMessageBuilder.getSourceAddr(),
            shortMessageBuilder.getDestAddrTon(),
            shortMessageBuilder.getDestAddrNpi(),
            shortMessageBuilder.getDestinationAddr(),
            shortMessageBuilder.getEsmClass(),
            shortMessageBuilder.getProtocolId(),
            shortMessageBuilder.getPriorityFlag(),
            shortMessageBuilder.getScheduleDeliveryTime(),
            shortMessageBuilder.getValidityPeriod(),
            shortMessageBuilder.getRegisteredDelivery(),
            shortMessageBuilder.getReplaceIfPresentFlag(),
            shortMessageBuilder.getDataCoding(),
            shortMessageBuilder.getSmDefaultMsgId(),
            shortMessageBuilder.getShortMessage(),
            shortMessageBuilder.getOptionalParameters().toArray(OctetString[]::new)
        );
        log.info("Message submitted, message_id is {}", messageId);
        return messageId;
    }

    private SmppShortMessageConfigHolder buildMessageParameters(String senderId, String destAddrs, byte deliveryReport,
                                                                CustomParametersBuilder customParameters,
                                                                DataCoding dataCoding, List<OctetString> optional) {
        Smpp smpp = appProps.getSmpp();
        Date scheduleDeliveryTime = new Date();

        return SmppShortMessageConfigHolder.builder()
            .serviceType(smpp.getServiceType())
            .sourceAddrTon(getSourceAddrTon(customParameters.getSourceTon(), smpp))
            .sourceAddrNpi(smpp.getSourceAddrNpi())
            .sourceAddr(getSourceAddr(senderId, smpp))
            .destAddrTon(getDestAddrTon(customParameters.getDestinationTon(), smpp))
            .destAddrNpi(smpp.getDestAddrNpi())
            .destinationAddr(destAddrs)
            .esmClass(new ESMClass())
            .protocolId((customParameters.getProtocolId() != null ? customParameters.getProtocolId().byteValue() :
                (byte) smpp.getProtocolId()))
            .priorityFlag((byte) smpp.getPriorityFlag())
            .scheduleDeliveryTime(timeFormatter.format(scheduleDeliveryTime))
            .validityPeriod(validityPeriodOrDefault(customParameters.getValidityPeriod(), scheduleDeliveryTime,
                IntegerValidator.getInstance().validate(smpp.getValidityPeriod())))
            .registeredDelivery(new RegisteredDelivery(deliveryReport))
            .replaceIfPresentFlag((byte) smpp.getReplaceIfPresentFlag())
            .dataCoding(dataCoding)
            .smDefaultMsgId((byte) smpp.getSmDefaultMsgId())
            .shortMessage(EMPTY_MESSAGE)
            .optionalParameters(optional)
            .build();
    }

    private String validityPeriodOrDefault(Integer requestedValidityPeriod,
                                           Date scheduleDeliveryTime, Integer defaultValidityPeriod) {
        Integer period = requestedValidityPeriod != null
            ? requestedValidityPeriod
            : defaultValidityPeriod;

        if (period != null) {
            return timeFormatter.format(new Date(scheduleDeliveryTime.getTime() + period * 1000));
        } else {
            return null;
        }
    }

    private DataCoding getDataCoding(String message) {
        return isAlpha(message)
            ? createEncoding(ALPHA_DEFAULT, appProps.getSmpp().getAlphaEncoding())
            : createEncoding(ALPHA_UCS2, appProps.getSmpp().getNotAlphaEncoding());
    }

    private DataCoding createEncoding(Alphabet defaultEncoding, Byte encoding) {
        return encoding == null ? new GeneralDataCoding(defaultEncoding, MessageClass.CLASS1, false) : () -> encoding;
    }

    private SMPPSession getActualSession() {
        SMPPSession session = this.session;
        if (session != null && session.getSessionState().isBound()) {
            return session;
        }

        synchronized (this) {
            session = this.session;
            if (session == null) {
                session = createSession(appProps);
                this.session = session;
            } else if (!session.getSessionState().isBound()) {
                session.unbindAndClose();
                session = createSession(appProps);
                this.session = session;
            }
        }
        return session;
    }

    private boolean isAlpha(String message) {
        return StandardCharsets.ISO_8859_1.newEncoder().canEncode(message);
    }

    private OctetString toPayload(String message) throws UnsupportedEncodingException {
        if (isAlpha(message)) {
            return new OctetString(MESSAGE_PAYLOAD.code(), message);
        } else {
            return new OctetString(MESSAGE_PAYLOAD.code(), message, UTF_16BE.name());
        }
    }

    private String getSourceAddr(String senderId, Smpp smpp) {
        return isBlank(senderId) ? smpp.getSourceAddr() : senderId;
    }

    public TypeOfNumber getSourceAddrTon(String sourceTon, Smpp smpp) {
        return isBlank(sourceTon) ? smpp.getSourceAddrTon() : TypeOfNumber.valueOf(sourceTon);
    }

    public TypeOfNumber getDestAddrTon(String destTon, Smpp smpp) {
        return isBlank(destTon) ? smpp.getDestAddrTon() : TypeOfNumber.valueOf(destTon);
    }

    @PreDestroy
    public void onDestroy() throws Exception {
        session.unbindAndClose();
    }

    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class CustomParametersBuilder {

        /**
         * Source address ton (optional, by default get from application.properties)
         */
        private String sourceTon;

        /**
         * Destination address ton (optional, by default get from application.properties)
         */
        private String destinationTon;

        /**
         * A number of seconds a message is valid. If is not delivered in this period will get EXPIRED delivery state
         * (uses the default if null).
         */
        private Integer validityPeriod;

        /**
         * Smpp protocol id (uses the default if null)
         */
        private Integer protocolId;
    }

    @Builder
    @Getter
    private static class SmppShortMessageConfigHolder {

        /**
         * Is the service_type. The service_type parameter can be used to indicate the SMS Application service
         * associated with the message.
         */
        private String serviceType;

        /**
         * Is the source_addr_ton. Type of Number for source address.
         */
        private TypeOfNumber sourceAddrTon;

        /**
         * Is the source_addr_npi. Numbering Plan Indicator for source address.
         */
        private NumberingPlanIndicator sourceAddrNpi;

        /**
         * Is the source_addr. Address of SME which originated this message
         */
        private String sourceAddr;

        /**
         * Is the dest_addr_ton. Type of Number for destination.
         */
        private TypeOfNumber destAddrTon;

        /**
         * Is the dest_addr_npi. Numbering Plan Indicator for destination.
         */
        private NumberingPlanIndicator destAddrNpi;

        /**
         * Is the destination_addr. Destination address of this short message.
         */
        private String destinationAddr;

        /**
         * Is the esm_class. Indicates Message Mode & Message Type.
         */
        private ESMClass esmClass;

        /**
         * Is the protocol_id. Protocol Identifier. Network specific field.
         */
        private byte protocolId;

        /**
         * Is the priority_flag. Designates the priority level of the message.
         */
        private byte priorityFlag;

        /**
         * Is the schedule_delivery_time. The short message is to be scheduled by the SMSC for delivery
         */
        private String scheduleDeliveryTime;

        /**
         * Is the validity_period. The validity period of this message.
         */
        private String validityPeriod;

        /**
         * Is the registered_delivery. Indicator to signify if an SMSC delivery receipt or an SME acknowledgement is required.
         */
        private RegisteredDelivery registeredDelivery;

        /**
         * Is the replace_if_present_flag.  Flag indicating if submitted message should replace an existing message.
         */
        private byte replaceIfPresentFlag;

        /**
         * Is the data_coding. Defines the encoding scheme of the short message user data.
         */
        private DataCoding dataCoding;

        /**
         * Is the sm_default_msg_id. Indicates the short message to send from a list of predefined (‘canned’) short messages stored on the SMSC.
         */
        private byte smDefaultMsgId;

        /**
         * Is the short_message. Use message_payload in optionalParameters
         */
        private byte[] shortMessage;

        /**
         * Is the optional parameters. See the specification
         */
        private List<OctetString> optionalParameters;
    }
}
