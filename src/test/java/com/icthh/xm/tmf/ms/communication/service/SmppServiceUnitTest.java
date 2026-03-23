package com.icthh.xm.tmf.ms.communication.service;

import static org.jsmpp.bean.Alphabet.ALPHA_DEFAULT;
import static org.jsmpp.bean.OptionalParameter.Tag.MESSAGE_PAYLOAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties.Smpp;
import com.icthh.xm.tmf.ms.communication.service.SmppService.CustomParametersBuilder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.SMPPSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SmppServiceUnitTest {

    private static final String DEST_ADDRESS = "380500000000";
    private static final String SENDER_ID = "TestSender";
    private static final String MESSAGE_TEXT = "Hello world";
    private static final String SMSC_MESSAGE_ID = "smsc-12345";
    private static final byte DELIVERY_REPORT = 1;
    private static final String SERVICE_TYPE = "CMT";
    private static final String SOURCE_ADDR = "DefaultSender";
    private static final String VALIDITY_PERIOD = "3600";
    private static final int PROTOCOL_ID = 0;
    private static final int PRIORITY_FLAG = 0;
    private static final int REPLACE_IF_PRESENT = 0;
    private static final int SM_DEFAULT_MSG_ID = 0;
    private static final Date CURRENT_DATE = Date.from(LocalDateTime.of(2026, 3, 13, 0, 0, 0)
        .toInstant(ZoneOffset.UTC));
    private static final String SCHEDULE_DELIVERY_TIME = "260313000000000+";
    private static final String EXPECTED_VALIDITY_PERIOD = "260313010000000+";
    private static final String EXPECTED_CUSTOM_VALIDITY_PERIOD = "260313020000000+";
    private static final ESMClass EXPECTED_TEXT_ESM = new ESMClass();
    private static final ESMClass EXPECTED_BINARY_ESM = new ESMClass(MessageMode.DEFAULT,
        org.jsmpp.bean.MessageType.DEFAULT,
        GSMSpecificFeature.UDHI);

    private static final Map<Short, String> OPTIONAL_PARAMETERS = Map.of((short) 6005, "30001");
    private static final byte[] EMPTY_MESSAGE = "".getBytes();

    private static final RegisteredDelivery EXPECTED_DELIVERY_REPORT = new RegisteredDelivery(DELIVERY_REPORT);
    private static final DataCoding EXPECTED_ALPHA_DATA_CONFIG = new GeneralDataCoding(ALPHA_DEFAULT,
        MessageClass.CLASS1, false);
    private static final byte EXPECTED_BINARY_DATA_CONFIG = (byte) 0x04;
    private static final OptionalParameter EXPECTED_OPTIONAL_PARAMETER = new OctetString((short) 6005,
        "30001".getBytes());
    private static final OptionalParameter EXPECTED_ALPHA_PAYLOAD_PARAMETER = new OctetString(MESSAGE_PAYLOAD.code(),
        MESSAGE_TEXT.getBytes());

    @Mock
    private ApplicationProperties appProps;
    @Mock
    private SMPPSession session;
    @Captor
    private ArgumentCaptor<ESMClass> esmClassCaptor;
    @Captor
    private ArgumentCaptor<DataCoding> dataCodingCaptor;

    private SmppService smppService;

    @BeforeEach
    void setUp() {
        List<DeliveryReportListener> listeners = Collections.emptyList();
        smppService = spy(new SmppService(appProps, listeners));
        injectBoundSession();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    void shouldReturnOverriddenSourceTon() {
        Smpp smpp = defaultSmpp();
        assertEquals(TypeOfNumber.INTERNATIONAL, smppService.getSourceAddrTon("INTERNATIONAL", smpp));
    }

    @Test
    void shouldReturnDefaultSourceTonWhenOverrideIsBlank() {
        Smpp smpp = defaultSmpp();
        smpp.setSourceAddrTon(TypeOfNumber.ALPHANUMERIC);
        assertEquals(TypeOfNumber.ALPHANUMERIC, smppService.getSourceAddrTon(null, smpp));
        assertEquals(TypeOfNumber.ALPHANUMERIC, smppService.getSourceAddrTon("", smpp));
    }

    @Test
    void shouldReturnOverriddenDestTon() {
        Smpp smpp = defaultSmpp();
        assertEquals(TypeOfNumber.INTERNATIONAL, smppService.getDestAddrTon("INTERNATIONAL", smpp));
    }

    @Test
    void shouldReturnDefaultDestTonWhenOverrideIsBlank() {
        Smpp smpp = defaultSmpp();
        smpp.setDestAddrTon(TypeOfNumber.NETWORK_SPECIFIC);
        assertEquals(TypeOfNumber.NETWORK_SPECIFIC, smppService.getDestAddrTon(null, smpp));
        assertEquals(TypeOfNumber.NETWORK_SPECIFIC, smppService.getDestAddrTon("", smpp));
    }

    @Test
    void shouldSubmitAlphaMessage() throws Exception {
        stubSmpp();
        doReturn(CURRENT_DATE).when(smppService).getCurrentDate();
        when(session.submitShortMessage(
            eq(SERVICE_TYPE), eq(TypeOfNumber.ALPHANUMERIC), eq(NumberingPlanIndicator.UNKNOWN),
            eq(SENDER_ID), eq(TypeOfNumber.UNKNOWN), eq(NumberingPlanIndicator.UNKNOWN),
            eq(DEST_ADDRESS), esmClassCaptor.capture(), eq((byte) PROTOCOL_ID), eq((byte) PRIORITY_FLAG),
            eq(SCHEDULE_DELIVERY_TIME),
            eq(EXPECTED_VALIDITY_PERIOD),
            eq(EXPECTED_DELIVERY_REPORT), eq((byte) REPLACE_IF_PRESENT),
            eq(EXPECTED_ALPHA_DATA_CONFIG), eq((byte) SM_DEFAULT_MSG_ID), eq(EMPTY_MESSAGE),
            eq(EXPECTED_OPTIONAL_PARAMETER), eq(EXPECTED_ALPHA_PAYLOAD_PARAMETER)
        )).thenReturn(SMSC_MESSAGE_ID);

        CustomParametersBuilder params = CustomParametersBuilder.builder().build();
        String messageId = smppService.send(DEST_ADDRESS, MESSAGE_TEXT, SENDER_ID,
            DELIVERY_REPORT, OPTIONAL_PARAMETERS, params);

        assertEquals(SMSC_MESSAGE_ID, messageId);
        assertEquals(EXPECTED_TEXT_ESM, esmClassCaptor.getValue());
    }

    @Test
    void shouldUseFallbackSourceAddrWhenSenderIdIsBlank() throws Exception {
        stubSmpp();
        when(session.submitShortMessage(
            anyString(), any(TypeOfNumber.class), any(NumberingPlanIndicator.class), eq(SOURCE_ADDR),
            any(TypeOfNumber.class), any(NumberingPlanIndicator.class), anyString(),
            any(ESMClass.class), anyByte(), anyByte(), anyString(), any(),
            any(RegisteredDelivery.class), anyByte(), any(DataCoding.class), anyByte(),
            any(byte[].class), any(OptionalParameter.class)
        )).thenReturn(SMSC_MESSAGE_ID);

        CustomParametersBuilder params = CustomParametersBuilder.builder().build();
        String messageId = smppService.send(DEST_ADDRESS, MESSAGE_TEXT, null,
            DELIVERY_REPORT, Map.of(), params);

        assertEquals(SMSC_MESSAGE_ID, messageId);
    }

    @Test
    void shouldApplyCustomProtocolIdAndValidityPeriod() throws Exception {
        stubSmpp();
        doReturn(CURRENT_DATE).when(smppService).getCurrentDate();
        int customProtocol = 64;
        int customValidity = 7200;

        when(session.submitShortMessage(
            anyString(), any(TypeOfNumber.class), any(NumberingPlanIndicator.class), anyString(),
            any(TypeOfNumber.class), any(NumberingPlanIndicator.class), anyString(),
            any(ESMClass.class), eq((byte) customProtocol), anyByte(), eq(SCHEDULE_DELIVERY_TIME),
            eq(EXPECTED_CUSTOM_VALIDITY_PERIOD),
            any(RegisteredDelivery.class), anyByte(), any(DataCoding.class), anyByte(),
            any(byte[].class), any(OptionalParameter.class)
        )).thenReturn(SMSC_MESSAGE_ID);

        CustomParametersBuilder params = CustomParametersBuilder.builder()
            .protocolId(customProtocol)
            .validityPeriod(customValidity)
            .build();

        String messageId = smppService.send(DEST_ADDRESS, MESSAGE_TEXT, SENDER_ID,
            DELIVERY_REPORT, Map.of(), params);

        assertEquals(SMSC_MESSAGE_ID, messageId);
    }

    @Test
    void shouldApplyCustomSourceAndDestTon() throws Exception {
        stubSmpp();
        when(session.submitShortMessage(
            anyString(), eq(TypeOfNumber.INTERNATIONAL), any(NumberingPlanIndicator.class), anyString(),
            eq(TypeOfNumber.NATIONAL), any(NumberingPlanIndicator.class), anyString(),
            any(ESMClass.class), anyByte(), anyByte(), anyString(), any(),
            any(RegisteredDelivery.class), anyByte(), any(DataCoding.class), anyByte(),
            any(byte[].class), any(OptionalParameter.class)
        )).thenReturn(SMSC_MESSAGE_ID);

        CustomParametersBuilder params = CustomParametersBuilder.builder()
            .sourceTon("INTERNATIONAL")
            .destinationTon("NATIONAL")
            .build();

        String messageId = smppService.send(DEST_ADDRESS, MESSAGE_TEXT, SENDER_ID, DELIVERY_REPORT, Map.of(), params);

        assertEquals(SMSC_MESSAGE_ID, messageId);
    }

    @Test
    void shouldSubmitBinaryMessage() throws Exception {
        stubSmpp();
        doReturn(CURRENT_DATE).when(smppService).getCurrentDate();
        byte[] binaryPayload = {0x01, 0x02, 0x03};

        when(session.submitShortMessage(
            eq(SERVICE_TYPE), eq(TypeOfNumber.ALPHANUMERIC), eq(NumberingPlanIndicator.UNKNOWN),
            eq(SENDER_ID), eq(TypeOfNumber.UNKNOWN), eq(NumberingPlanIndicator.UNKNOWN),
            eq(DEST_ADDRESS), esmClassCaptor.capture(), eq((byte) PROTOCOL_ID), eq((byte) PRIORITY_FLAG),
            eq(SCHEDULE_DELIVERY_TIME),
            eq(EXPECTED_VALIDITY_PERIOD),
            eq(EXPECTED_DELIVERY_REPORT), eq((byte) REPLACE_IF_PRESENT),
            dataCodingCaptor.capture(), eq((byte) SM_DEFAULT_MSG_ID),
            eq(binaryPayload), eq(EXPECTED_OPTIONAL_PARAMETER)
        )).thenReturn(SMSC_MESSAGE_ID);

        CustomParametersBuilder params = CustomParametersBuilder.builder().build();
        String messageId = smppService.sendBinary(DEST_ADDRESS, SENDER_ID, DELIVERY_REPORT,
            binaryPayload, params, OPTIONAL_PARAMETERS);

        assertEquals(SMSC_MESSAGE_ID, messageId);
        assertEquals(EXPECTED_BINARY_ESM, esmClassCaptor.getValue());
        assertEquals(EXPECTED_BINARY_DATA_CONFIG, dataCodingCaptor.getValue().toByte());
    }

    @Test
    void shouldNotCreateSessionWhenSmppDisabled() {
        Smpp smpp = defaultSmpp();
        smpp.setEnabled(false);
        when(appProps.getSmpp()).thenReturn(smpp);

        smppService.init();

        verify(session, never()).getSessionState();
    }

    @Test
    void shouldUnbindAndCloseSessionOnDestroy() throws Exception {
        smppService.onDestroy();
        verify(session).unbindAndClose();
    }

    @Test
    void shouldHandleNullSessionOnDestroy() throws Exception {
        ReflectionTestUtils.setField(smppService, "session", null);
        smppService.onDestroy();
    }

    private void injectBoundSession() {
        lenient().when(session.getSessionState()).thenReturn(SessionState.BOUND_TRX);
        ReflectionTestUtils.setField(smppService, "session", session);
    }

    private void stubSmpp() {
        Smpp smpp = defaultSmpp();
        when(appProps.getSmpp()).thenReturn(smpp);
    }

    private static Smpp defaultSmpp() {
        Smpp smpp = new Smpp();
        smpp.setEnabled(true);
        smpp.setServiceType(SERVICE_TYPE);
        smpp.setSourceAddr(SOURCE_ADDR);
        smpp.setSourceAddrTon(TypeOfNumber.ALPHANUMERIC);
        smpp.setSourceAddrNpi(NumberingPlanIndicator.UNKNOWN);
        smpp.setDestAddrTon(TypeOfNumber.UNKNOWN);
        smpp.setDestAddrNpi(NumberingPlanIndicator.UNKNOWN);
        smpp.setProtocolId(PROTOCOL_ID);
        smpp.setPriorityFlag(PRIORITY_FLAG);
        smpp.setReplaceIfPresentFlag(REPLACE_IF_PRESENT);
        smpp.setSmDefaultMsgId(SM_DEFAULT_MSG_ID);
        smpp.setValidityPeriod(VALIDITY_PERIOD);
        return smpp;
    }
}
