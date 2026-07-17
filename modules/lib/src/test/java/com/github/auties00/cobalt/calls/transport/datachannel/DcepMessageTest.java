package com.github.auties00.cobalt.calls.transport.datachannel;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DcepMessage RFC 8832 codec")
class DcepMessageTest {
    @Nested
    @DisplayName("DATA_CHANNEL_OPEN")
    class OpenCodec {
        @Test
        @DisplayName("encodes the canonical reliable-ordered header then label then protocol")
        void encodeReliable() {
            var open = new DcepMessage.Open(DcepMessage.CHANNEL_RELIABLE, 0, 0L, "data", "wa");
            var bytes = open.encode();
            var label = "data".getBytes(StandardCharsets.UTF_8);
            var protocol = "wa".getBytes(StandardCharsets.UTF_8);
            assertEquals(DcepMessage.OPEN_HEADER_SIZE + label.length + protocol.length, bytes.length);
            assertEquals(DcepMessage.MSG_OPEN, bytes[0]);
            assertEquals(DcepMessage.CHANNEL_RELIABLE, bytes[1]);
            // priority u16 (bytes 2..3), reliability u32 (4..7), labelLen u16 (8..9), protoLen u16 (10..11)
            assertEquals(label.length, (bytes[8] & 0xFF) << 8 | (bytes[9] & 0xFF), "label length is big-endian");
            assertEquals(protocol.length, (bytes[10] & 0xFF) << 8 | (bytes[11] & 0xFF), "protocol length is big-endian");
            assertArrayEquals(label, Arrays.copyOfRange(bytes, 12, 12 + label.length));
            assertArrayEquals(protocol, Arrays.copyOfRange(bytes, 12 + label.length, bytes.length));
        }

        @Test
        @DisplayName("round-trips every field through encode and decode")
        void roundTrip() {
            var open = new DcepMessage.Open(
                    DcepMessage.CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED, 1024, 7L, "control", "subproto");
            var decoded = assertInstanceOf(DcepMessage.Open.class, DcepMessage.decode(open.encode()));
            assertEquals(open, decoded);
            assertTrue(decoded.unordered());
            assertEquals(7, decoded.maxRetransmits().orElseThrow());
            assertTrue(decoded.maxLifetimeMs().isEmpty());
        }

        @Test
        @DisplayName("interprets the reliability parameter as a lifetime for the timed channel type")
        void timedReliability() {
            var open = new DcepMessage.Open(DcepMessage.CHANNEL_PARTIAL_RELIABLE_TIMED, 0, 3000L, "x", "");
            assertEquals(3000, open.maxLifetimeMs().orElseThrow());
            assertTrue(open.maxRetransmits().isEmpty());
            assertFalse(open.unordered());
        }

        @Test
        @DisplayName("rejects a payload shorter than the fixed header")
        void rejectShortHeader() {
            var truncated = new byte[DcepMessage.OPEN_HEADER_SIZE - 1];
            truncated[0] = DcepMessage.MSG_OPEN;
            assertThrows(WhatsAppCallException.DataChannel.class, () -> DcepMessage.decode(truncated));
        }

        @Test
        @DisplayName("rejects a payload shorter than its declared label and protocol lengths")
        void rejectTruncatedBody() {
            var open = new DcepMessage.Open(DcepMessage.CHANNEL_RELIABLE, 0, 0L, "longlabel", "proto");
            var full = open.encode();
            var truncated = Arrays.copyOf(full, full.length - 3);
            assertThrows(WhatsAppCallException.DataChannel.class, () -> DcepMessage.decode(truncated));
        }
    }

    @Nested
    @DisplayName("DATA_CHANNEL_ACK")
    class AckCodec {
        @Test
        @DisplayName("encodes to the single ACK message-type byte")
        void encode() {
            assertArrayEquals(new byte[]{DcepMessage.MSG_ACK}, DcepMessage.Ack.INSTANCE.encode());
        }

        @Test
        @DisplayName("decodes a single ACK byte to the cached singleton")
        void decodeSingleton() {
            assertSame(DcepMessage.Ack.INSTANCE, DcepMessage.decode(new byte[]{DcepMessage.MSG_ACK}));
        }
    }

    @Nested
    @DisplayName("decode guards")
    class DecodeGuards {
        @Test
        @DisplayName("rejects an empty payload")
        void rejectEmpty() {
            assertThrows(WhatsAppCallException.DataChannel.class, () -> DcepMessage.decode(new byte[0]));
        }

        @ParameterizedTest
        @ValueSource(ints = {0x00, 0x01, 0x04, 0xFF})
        @DisplayName("rejects an unknown leading message-type byte")
        void rejectUnknownType(int leadingByte) {
            assertThrows(WhatsAppCallException.DataChannel.class,
                    () -> DcepMessage.decode(new byte[]{(byte) leadingByte}));
        }
    }

    @Nested
    @DisplayName("channel-type helpers")
    class ChannelTypeHelpers {
        @ParameterizedTest
        @ValueSource(ints = {0x80, 0x81, 0x82})
        @DisplayName("flags the unordered channel types by their high bit")
        void unordered(int channelType) {
            assertTrue(DcepMessage.isUnordered((byte) channelType));
        }

        @ParameterizedTest
        @ValueSource(ints = {0x00, 0x01, 0x02})
        @DisplayName("does not flag the ordered channel types")
        void ordered(int channelType) {
            assertFalse(DcepMessage.isUnordered((byte) channelType));
        }
    }
}
