package com.icthh.xm.tmf.ms.communication.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class WapPushSegmentationServiceUnitTest {

    private static final int DEFAULT_PORT = 2948;
    private static final int MAX_BYTES_PER_SEGMENT = 128;
    private static final int UDH_LENGTH = 12;
    private static final int MAX_SEGMENTS = 255;

    private static final int UDH_IDX_UDHL = 0;
    private static final int UDH_IDX_IEI_PORT = 1;
    private static final int UDH_IDX_IE_PORT_LEN = 2;
    private static final int UDH_IDX_DEST_PORT_HIGH = 3;
    private static final int UDH_IDX_DEST_PORT_LOW = 4;
    private static final int UDH_IDX_SRC_PORT_HIGH = 5;
    private static final int UDH_IDX_SRC_PORT_LOW = 6;
    private static final int UDH_IDX_IEI_CONCAT = 7;
    private static final int UDH_IDX_IE_CONCAT_LEN = 8;
    private static final int UDH_IDX_REF = 9;
    private static final int UDH_IDX_TOTAL = 10;
    private static final int UDH_IDX_PART = 11;

    private static final byte UDHL_VALUE = (byte) 0x0B;
    private static final byte IEI_PORT_VALUE = (byte) 0x05;
    private static final byte IE_PORT_LEN_VALUE = (byte) 0x04;
    private static final byte IEI_CONCAT_VALUE = (byte) 0x00;
    private static final byte IE_CONCAT_LEN_VALUE = (byte) 0x03;

    private final WapPushSegmentationService unit = new WapPushSegmentationService(new ApplicationProperties());

    private static byte[] udh(byte[] segment) {
        return Arrays.copyOfRange(segment, 0, UDH_LENGTH);
    }

    private static byte[] chunk(byte[] segment) {
        return Arrays.copyOfRange(segment, UDH_LENGTH, segment.length);
    }

    private static int portFrom(byte[] udh, int highIdx, int lowIdx) {
        return ((udh[highIdx] & 0xFF) << 8) | (udh[lowIdx] & 0xFF);
    }

    static Stream<String> invalidInputs() {
        return Stream.of(
            null,
            "   ",
            "ABC",
            "0XGG"
        );
    }

    @ParameterizedTest(name = "[{index}] input=\"{0}\"")
    @MethodSource("invalidInputs")
    void shouldThrowIllegalArgumentExceptionWhenInputIsInvalid(String hex) {
        assertThrows(IllegalArgumentException.class, () -> unit.buildWapPushSegmentDetails(hex));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenPayloadExceedsMaximum() {
        String hex = "AA".repeat(MAX_SEGMENTS * MAX_BYTES_PER_SEGMENT + 1);
        assertThrows(IllegalArgumentException.class, () -> unit.buildWapPushSegmentDetails(hex));
    }

    @ParameterizedTest(name = "payload {0} bytes → {1} segment(s), last chunk {2} bytes")
    @CsvSource({
        "10,  1,  10",
        "128, 1, 128",
        "129, 2,   1",
        "300, 3,  44"
    })
    void shouldProduceCorrectSegmentationWhenPayloadVariesInSize(int payloadBytes, int expectedSegments,
        int expectedLastChunk) {
        List<byte[]> segments = unit.buildWapPushSegmentDetails("01".repeat(payloadBytes));

        assertEquals(expectedSegments, segments.size());
        assertEquals(expectedLastChunk, chunk(segments.get(segments.size() - 1)).length);
        segments.forEach(s -> assertEquals(expectedSegments, s[UDH_IDX_TOTAL] & 0xFF));

        for (int i = 0; i < segments.size(); i++) {
            assertEquals(i + 1, segments.get(i)[UDH_IDX_PART] & 0xFF);
        }
        for (int i = 0; i < segments.size() - 1; i++) {
            assertEquals(MAX_BYTES_PER_SEGMENT, chunk(segments.get(i)).length);
        }

        int ref = segments.get(0)[UDH_IDX_REF] & 0xFF;
        segments.forEach(s -> assertEquals(ref, s[UDH_IDX_REF] & 0xFF));
    }

    @Test
    void shouldBuildCorrectUdhStructureAndFullDataWhenSegmentIsConstructed() {

        byte[] seg = unit.buildWapPushSegmentDetails("EE".repeat(3)).get(0);
        byte[] expectedSegment = new byte[]{((byte) 238), ((byte) 238), ((byte) 238)};
        byte[] udh = udh(seg);
        byte[] chunk = chunk(seg);

        assertEquals(UDH_LENGTH, udh.length);
        assertEquals(UDHL_VALUE, udh[UDH_IDX_UDHL]);
        assertEquals(IEI_PORT_VALUE, udh[UDH_IDX_IEI_PORT]);
        assertEquals(IE_PORT_LEN_VALUE, udh[UDH_IDX_IE_PORT_LEN]);
        assertEquals(IEI_CONCAT_VALUE, udh[UDH_IDX_IEI_CONCAT]);
        assertEquals(IE_CONCAT_LEN_VALUE, udh[UDH_IDX_IE_CONCAT_LEN]);
        assertEquals(DEFAULT_PORT, portFrom(udh, UDH_IDX_DEST_PORT_HIGH, UDH_IDX_DEST_PORT_LOW));
        assertEquals(DEFAULT_PORT, portFrom(udh, UDH_IDX_SRC_PORT_HIGH, UDH_IDX_SRC_PORT_LOW));

        assertEquals(UDH_LENGTH + chunk.length, seg.length);
        assertArrayEquals(chunk, expectedSegment);
    }
}
