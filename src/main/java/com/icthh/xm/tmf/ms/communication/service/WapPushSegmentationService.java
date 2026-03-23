package com.icthh.xm.tmf.ms.communication.service;

import com.icthh.xm.tmf.ms.communication.config.ApplicationProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WapPushSegmentationService {

    private static final int MAX_PAYLOAD_BYTES_PER_SEGMENT = 128;
    private static final int UDH_LENGTH = 12;
    private static final int MAX_SEGMENTS = 255;

    private final ApplicationProperties.WapPush wapPush;

    private final AtomicInteger referenceCounter = new AtomicInteger(0);

    public WapPushSegmentationService(ApplicationProperties applicationProperties) {
        this.wapPush = applicationProperties.getWapPush();
    }

    public List<byte[]> buildWapPushSegmentDetails(String hexPayload) {
        return buildWapPushSegmentDetails(hexPayload, wapPush.getDestinationPort(), wapPush.getSourcePort());
    }

    private List<byte[]> buildWapPushSegmentDetails(String hexPayload, int destinationPort, int sourcePort) {
        byte[] payload = hexToBytes(hexPayload);

        int totalParts = (payload.length + MAX_PAYLOAD_BYTES_PER_SEGMENT - 1) / MAX_PAYLOAD_BYTES_PER_SEGMENT;
        if (totalParts == 0) {
            totalParts = 1;
        }
        if (totalParts > MAX_SEGMENTS) {
            throw new IllegalArgumentException(
                "Payload is too large: requires " + totalParts + " segments, maximum is " + MAX_SEGMENTS);
        }

        int reference = nextReference();
        log.debug("Segmenting WAP Push payload: payloadBytes={}, totalParts={}, reference={}, destPort={}, srcPort={}",
            payload.length, totalParts, String.format("0x%02X", reference), destinationPort, sourcePort);

        List<byte[]> segments = new ArrayList<>(totalParts);
        for (int i = 0; i < totalParts; i++) {
            int partNumber = i + 1;
            int offset = i * MAX_PAYLOAD_BYTES_PER_SEGMENT;
            int chunkLength = Math.min(MAX_PAYLOAD_BYTES_PER_SEGMENT, payload.length - offset);

            byte[] chunk = new byte[chunkLength];
            System.arraycopy(payload, offset, chunk, 0, chunkLength);

            byte[] udh = buildUdh((byte) reference, (byte) totalParts, (byte) partNumber,
                destinationPort, sourcePort);

            byte[] fullData = new byte[UDH_LENGTH + chunkLength];
            System.arraycopy(udh, 0, fullData, 0, UDH_LENGTH);
            System.arraycopy(chunk, 0, fullData, UDH_LENGTH, chunkLength);

            segments.add(fullData);
        }

        return segments;
    }

    private byte[] buildUdh(byte reference, byte totalParts, byte partNumber,
        int destinationPort, int sourcePort) {
        return new byte[]{
            (byte) 0x0B,                             // UDHL: length of remaining UDH = 11
            (byte) 0x05,                             // IEI: 16-bit application port addressing
            (byte) 0x04,                             // IE data length
            (byte) ((destinationPort >> 8) & 0xFF),  // destination port high byte
            (byte) (destinationPort & 0xFF),         // destination port low byte
            (byte) ((sourcePort >> 8) & 0xFF),       // source port high byte
            (byte) (sourcePort & 0xFF),              // source port low byte
            (byte) 0x00,                             // IEI: 8-bit concatenated SMS
            (byte) 0x03,                             // IE data length
            reference,                               // RR - reference number
            totalParts,                              // TT - total number of parts
            partNumber                               // NN - current part number (1-based)
        };
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isBlank()) {
            throw new IllegalArgumentException("Hex payload must not be null or empty");
        }

        try {
            return Hex.decodeHex(hex);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid hex string: " + e.getMessage(), e);
        }
    }

    private int nextReference() {
        return referenceCounter.getAndUpdate(n -> (n + 1) & 0xFF);
    }
}
