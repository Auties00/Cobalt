package com.github.auties00.cobalt.calls.transport.rtcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the outbound RTCP serializers produce the exact byte-pinned sizes and fixed header fields the
 * WhatsApp relay leg emits: a seventy-six-byte two-block Sender Report followed by a thirty-two-byte Source
 * Description, a twenty-eight-byte {@code "HBHS"} application-layer feedback record, and a sixteen-byte
 * generic NACK with the fixed sender SSRC.
 */
@DisplayName("RtcpReportBuilder")
class RtcpReportBuilderTest {
    private static final byte[] CNAME =
            "abcdefghijklmnopqr".getBytes(StandardCharsets.US_ASCII); // 18 bytes -> 32-byte SDES record

    @Test
    @DisplayName("a two-block Sender Report plus Source Description compound is 76 + 32 bytes with the fixed SR and SDES headers")
    void senderReportWithSdes() {
        var blocks = List.of(
                new RtcpReportBuilder.ReportBlock(0x11111111, 0, 0, 0x1000, 0, 0, 0),
                new RtcpReportBuilder.ReportBlock(0x22222222, 0, 0, 0x2000, 0, 0, 0));
        var compound = RtcpReportBuilder.buildSenderReportWithSdes(
                0xAABBCCDD, 0x0102030405060708L, 0x0A0B0C0DL, 7, 1234, blocks, CNAME);

        assertEquals(76 + 32, compound.length, "compound length");
        // Sender Report header: V=2/P=0/RC=2 = 0x82, PT 200 (0xC8), length 18 words (76 bytes).
        assertEquals(0x82, compound[0] & 0xFF);
        assertEquals(0xC8, compound[1] & 0xFF);
        assertEquals(18, readU16(compound, 2));
        assertEquals(0xAABBCCDDL, readU32(compound, 4));
        assertEquals(0x01020304L, readU32(compound, 8));   // NTP high word
        assertEquals(0x05060708L, readU32(compound, 12));  // NTP low word
        assertEquals(0x0A0B0C0DL, readU32(compound, 16));  // RTP timestamp
        assertEquals(7L, readU32(compound, 20));           // sender packet count
        assertEquals(1234L, readU32(compound, 24));        // sender octet count
        assertEquals(0x11111111L, readU32(compound, 28));  // report block 1 SSRC
        assertEquals(0x1000L, readU32(compound, 36));      // report block 1 extended highest seq
        assertEquals(0x22222222L, readU32(compound, 52));  // report block 2 SSRC
        assertEquals(0x2000L, readU32(compound, 60));      // report block 2 extended highest seq

        // Source Description record starts at offset 76: V=2/SC=1 = 0x81, PT 202 (0xCA), length 7 words.
        assertEquals(0x81, compound[76] & 0xFF);
        assertEquals(0xCA, compound[77] & 0xFF);
        assertEquals(7, readU16(compound, 78));
        assertEquals(0xAABBCCDDL, readU32(compound, 80)); // same SSRC as the Sender Report
        assertEquals(0x01, compound[84] & 0xFF);          // CNAME item type
        assertEquals(18, compound[85] & 0xFF);            // CNAME length
        assertArrayEquals(CNAME, java.util.Arrays.copyOfRange(compound, 86, 86 + 18));
        assertEquals(0x00, compound[104] & 0xFF);         // END octet terminating the item list
    }

    @Test
    @DisplayName("a Source Description over an 18-byte CNAME is exactly 32 bytes")
    void sdesChunkSize() {
        var sdes = RtcpReportBuilder.buildSdesChunk(0x01020304, CNAME);
        assertEquals(32, sdes.length);
        assertEquals(0x81, sdes[0] & 0xFF);
        assertEquals(0xCA, sdes[1] & 0xFF);
        assertEquals(7, readU16(sdes, 2));
    }

    @Test
    @DisplayName("an application-layer feedback record is 28 bytes carrying the HBHS tag and the packed indices")
    void afbRecord() {
        var afb = RtcpReportBuilder.buildAfb(0x0A0A0A0A, 0x0B0B0B0B, 0x0000_1234_5678L, 0x009ABCDE);
        assertEquals(28, afb.length);
        assertEquals(0x8F, afb[0] & 0xFF);                 // V=2/FMT=15
        assertEquals(0xCE, afb[1] & 0xFF);                 // PT 206
        assertEquals(6, readU16(afb, 2));                  // length 6 words (28 bytes)
        assertEquals(0x0A0A0A0AL, readU32(afb, 4));        // sender SSRC
        assertEquals(0x0B0B0B0BL, readU32(afb, 8));        // media SSRC
        assertEquals(0x48424853L, readU32(afb, 12));       // ASCII 'HBHS'
        assertEquals(10, readU16(afb, 16));                // serialized payload length
        assertEquals(0x1234_5678L, read48(afb, 18));       // 48-bit RTP packet index
        assertEquals(0x009ABCDEL, readU32(afb, 24));       // 32-bit SRTCP index
    }

    @Test
    @DisplayName("a generic NACK is 16 bytes with the fixed sender SSRC 1 and the supplied PID and BLP")
    void nackRecord() {
        var nack = RtcpReportBuilder.buildNack(0x0C0C0C0C, 0x1234, 0xABCD);
        assertEquals(16, nack.length);
        assertEquals(0x81, nack[0] & 0xFF);                // V=2/FMT=1
        assertEquals(0xCD, nack[1] & 0xFF);                // PT 205
        assertEquals(3, readU16(nack, 2));                 // length 3 words (16 bytes)
        assertEquals(0x00000001L, readU32(nack, 4));       // fixed sender SSRC
        assertEquals(0x0C0C0C0CL, readU32(nack, 8));       // media SSRC
        assertEquals(0x1234, readU16(nack, 12));           // PID
        assertEquals(0xABCD, readU16(nack, 14));           // BLP
    }

    private static int readU16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static long readU32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    private static long read48(byte[] data, int offset) {
        var value = 0L;
        for (var i = 0; i < 6; i++) {
            value = (value << 8) | (data[offset + i] & 0xFF);
        }
        return value;
    }
}
