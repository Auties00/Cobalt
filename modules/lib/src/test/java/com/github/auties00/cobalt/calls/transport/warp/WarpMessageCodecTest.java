package com.github.auties00.cobalt.calls.transport.warp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the WARP media-control codec against SPEC section 14.1: the header type, the attribute
 * flag bits and their ascending-bit append order, the packed length field, the participant-report
 * block layout, and the piggyback-versus-standalone bandwidth-report rule. The expected bytes are
 * derived from the spec grammar by hand so a flag-order or length-packing regression is caught.
 */
@DisplayName("WARP message codec")
class WarpMessageCodecTest {
    @Nested
    @DisplayName("participant report block")
    class ParticipantReportBlock {
        @Test
        @DisplayName("writes the ten fields big-endian in declared order")
        void writesBlock() {
            var report = new WarpParticipantReport(0x0102, 0x0304, 0x05, 0x0607, 0x08, 0x090A);
            var out = new byte[WarpParticipantReport.BYTE_LENGTH];
            var end = report.writeTo(out, 0);
            assertEquals(WarpParticipantReport.BYTE_LENGTH, end);
            assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A}, out);
        }

        @Test
        @DisplayName("reads back what it wrote")
        void roundTrips() {
            var report = new WarpParticipantReport(40, 1200, 128, 7, 0x13, 800);
            var out = new byte[WarpParticipantReport.BYTE_LENGTH];
            report.writeTo(out, 0);
            assertEquals(report, WarpParticipantReport.readFrom(out, 0));
        }

        @Test
        @DisplayName("clamps a loss rate above one to the Q8 maximum of 255")
        void clampsLoss() {
            var report = WarpParticipantReport.ofLossRate(0, 0, 0, 0, 2.0, 0);
            assertEquals(WarpParticipantReport.MAX_PACKET_LOSS_Q8, report.packetLossQ8());
        }

        @Test
        @DisplayName("converts a half loss rate to Q8 128")
        void convertsLoss() {
            var report = WarpParticipantReport.ofLossRate(0, 0, 0, 0, 0.5, 0);
            assertEquals(128, report.packetLossQ8());
        }

        @Test
        @DisplayName("rejects a field outside its unsigned width")
        void rejectsOverflow() {
            assertThrows(IllegalArgumentException.class,
                    () -> new WarpParticipantReport(0x10000, 0, 0, 0, 0, 0));
        }
    }

    @Nested
    @DisplayName("message header and flags")
    class HeaderAndFlags {
        @Test
        @DisplayName("encodes a sequence-only piggyback with type 9 at byte 0 and the sequence flag at byte 4")
        void encodesSequenceOnly() {
            var message = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(0x1234)));
            var bytes = message.encode();
            assertEquals(WarpMessage.WARP_TYPE, bytes[0] & 0xff);
            assertEquals(WarpAttributeFlag.SEQUENCE_NUMBER.mask(), bytes[4] & 0xff);
            assertEquals(0x12, bytes[5] & 0xff);
            assertEquals(0x34, bytes[6] & 0xff);
            // payload = flags(1) + SEQ(2) = 3 (odd) -> padded to 4; byte1 = 4>>1 = 2.
            assertEquals(0x02, bytes[1] & 0xff, "byte 1 packs the padded payload length over two");
        }

        @Test
        @DisplayName("sets the participant-report companion bit so the report mask is 0x28")
        void setsParticipantReportCompanion() {
            var report = new WarpParticipantReport(1, 2, 3, 4, 5, 6);
            var message = new WarpMessage.Piggybacked(List.of(new WarpAttribute.ParticipantReport(report)));
            var bytes = message.encode();
            var expectedMask = WarpAttributeFlag.PARTICIPANT_REPORT.mask()
                    | WarpAttributeFlag.PARTICIPANT_REPORT_COMPANION.mask();
            assertEquals(expectedMask, bytes[4] & 0xff);
        }

        @Test
        @DisplayName("sets the extension flag and extension byte for a standalone bandwidth report")
        void setsExtensionByte() {
            var message = new WarpMessage.Standalone(List.of(new WarpAttribute.BandwidthReport(2, 1, 300)));
            var bytes = message.encode();
            assertTrue(WarpAttributeFlag.EXT_FLAG.isSet(bytes[4] & 0xff));
            assertEquals((WarpAttributeFlag.BANDWIDTH_REPORT.mask() >>> 8) & 0xff, bytes[5] & 0xff);
        }

        @Test
        @DisplayName("pads an odd attribute-byte count to an even total length")
        void padsToEven() {
            var message = new WarpMessage.Piggybacked(List.of(new WarpAttribute.VideoEncoding(0x03)));
            var bytes = message.encode();
            assertEquals(0, bytes.length % 2);
        }
    }

    @Nested
    @DisplayName("rolling-clock timestamp field")
    class TimestampField {
        @Test
        @DisplayName("defaults the timestamp to zero on the no-argument encode")
        void defaultsToZero() {
            var bytes = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(1))).encode();
            assertEquals(0, bytes[WarpMessage.TIMESTAMP_OFFSET] & 0xff);
            assertEquals(0, bytes[WarpMessage.TIMESTAMP_OFFSET + 1] & 0xff);
        }

        @Test
        @DisplayName("writes the timestamp big-endian at offsets two and three")
        void writesBigEndian() {
            var bytes = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(1))).encode(0x2846);
            // The captured standalone WARP led with 09 07 28 46: htons(10310) -> 28 46 at bytes 2 and 3.
            assertEquals(0x28, bytes[WarpMessage.TIMESTAMP_OFFSET] & 0xff);
            assertEquals(0x46, bytes[WarpMessage.TIMESTAMP_OFFSET + 1] & 0xff);
        }

        @Test
        @DisplayName("masks a timestamp wider than sixteen bits to its low sixteen bits")
        void masksToSixteenBits() {
            var bytes = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(1))).encode(0x1_2846);
            assertEquals(0x28, bytes[WarpMessage.TIMESTAMP_OFFSET] & 0xff);
            assertEquals(0x46, bytes[WarpMessage.TIMESTAMP_OFFSET + 1] & 0xff);
        }

        @Test
        @DisplayName("leaves the type, packed length, and flag bytes unchanged by the timestamp")
        void leavesOtherHeaderBytes() {
            var attributes = List.<WarpAttribute>of(new WarpAttribute.SequenceNumber(1));
            var withZero = new WarpMessage.Piggybacked(attributes).encode();
            var withTimestamp = new WarpMessage.Piggybacked(attributes).encode(0x9C40);
            assertEquals(withZero[0], withTimestamp[0]);
            assertEquals(withZero[1], withTimestamp[1]);
            assertEquals(withZero[WarpMessage.FLAGS_OFFSET], withTimestamp[WarpMessage.FLAGS_OFFSET]);
            assertEquals(withZero.length, withTimestamp.length);
        }

        @Test
        @DisplayName("decodes a message regardless of its timestamp value")
        void decodeIgnoresTimestamp() {
            var encoded = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(0x1234))).encode(0x5678);
            var decoded = WarpMessage.decode(encoded);
            assertInstanceOf(WarpMessage.Piggybacked.class, decoded);
            assertEquals(new WarpAttribute.SequenceNumber(0x1234), decoded.attributes().getFirst());
        }
    }

    @Nested
    @DisplayName("encode then decode")
    class EncodeDecode {
        @Test
        @DisplayName("round-trips a multi-attribute piggyback by reconstructing every attribute")
        void roundTripsPiggyback() {
            var report = new WarpParticipantReport(40, 1200, 200, 9, 0x13, 800);
            var attributes = List.<WarpAttribute>of(
                    new WarpAttribute.SequenceNumber(0xABCD),
                    new WarpAttribute.DownlinkBandwidth(1500),
                    new WarpAttribute.VideoEncoding(0x09),
                    new WarpAttribute.ParticipantReport(report),
                    new WarpAttribute.SenderBandwidthAllocation(0x11223344));
            var encoded = new WarpMessage.Piggybacked(attributes).encode();
            var decoded = WarpMessage.decode(encoded);
            assertInstanceOf(WarpMessage.Piggybacked.class, decoded);
            assertEquals(5, decoded.attributes().size());
            assertEquals(new WarpAttribute.SequenceNumber(0xABCD), decoded.attributes().get(0));
            assertEquals(new WarpAttribute.DownlinkBandwidth(1500), decoded.attributes().get(1));
            assertEquals(new WarpAttribute.VideoEncoding(0x09), decoded.attributes().get(2));
            assertEquals(new WarpAttribute.ParticipantReport(report), decoded.attributes().get(3));
            assertEquals(new WarpAttribute.SenderBandwidthAllocation(0x11223344), decoded.attributes().get(4));
        }

        @Test
        @DisplayName("decodes a standalone bandwidth report as a Standalone message")
        void decodesStandalone() {
            var encoded = BweConfigSender.encode(1, 300);
            var decoded = WarpMessage.decode(encoded);
            assertInstanceOf(WarpMessage.Standalone.class, decoded);
            var report = assertInstanceOf(WarpAttribute.BandwidthReport.class, decoded.attributes().getFirst());
            assertEquals(BweConfigSender.BWE_CONFIG_VERSION, report.version());
            assertEquals(1, report.index());
            assertEquals(300, report.minRemoteBweKbps());
        }

        @Test
        @DisplayName("rejects a message whose byte-zero type is not 9")
        void rejectsWrongType() {
            var bytes = new byte[]{0x08, 0x00, 0x00, 0x00, 0x00};
            assertThrows(IllegalArgumentException.class, () -> WarpMessage.decode(bytes));
        }

        @Test
        @DisplayName("rejects a piggyback constructed with a bandwidth-report attribute")
        void rejectsBandwidthReportOnPiggyback() {
            assertThrows(IllegalArgumentException.class,
                    () -> new WarpMessage.Piggybacked(List.of(new WarpAttribute.BandwidthReport(2, 0, 100))));
        }
    }
}
