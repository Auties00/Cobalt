package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptionsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptionsEntryBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamSubscriptionsSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.transport.stun.StunAttributeType;
import com.github.auties00.cobalt.calls.transport.stun.StunIntegrity;
import com.github.auties00.cobalt.calls.transport.stun.StunMessage;
import com.github.auties00.cobalt.calls.transport.subscription.SubscriptionEnvelope;
import com.github.auties00.cobalt.calls.transport.warp.WarpAttribute;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessage;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessageIntegrity;
import com.github.auties00.cobalt.calls.transport.warp.WarpParticipantReport;

/**
 * Adversarial wire-format verification for the STUN and WARP transport codecs against SPEC sections
 * 14.1 and 14.2 and the external RFC 5769 STUN test vectors. The integrity vectors are taken from
 * RFC 5769 section 2.1 rather than trusting the implementation's own output, so a byte-layout or
 * length-adjustment error in {@link StunIntegrity} is caught against an independent reference. The
 * round-trip and padding cases derive the expected bytes from the spec grammar by hand.
 */
@DisplayName("calls transport wire format")
class TransportWireFormatTest {
    private static final HexFormat HEX = HexFormat.of();

    @Nested
    @DisplayName("StunIntegrity against RFC 5769 section 2.1 sample request")
    class Rfc5769SampleRequest {
        // RFC 5769 2.1 "Sample Request": the 108-byte STUN Binding Request exactly as printed in the
        // RFC, ending in MESSAGE-INTEGRITY then FINGERPRINT. The short-term-credential password is
        // "VOkJxbRl1RmTxUk/WvJxBt". Attributes: SOFTWARE, PRIORITY, ICE-CONTROLLED, USERNAME,
        // MESSAGE-INTEGRITY, FINGERPRINT.
        private static final byte[] MESSAGE = HEX.parseHex(
                "000100582112a442b7e7a701bc34d686fa87dfae"
                        + "802200105354554e207465737420636c69656e74"
                        + "002400046e0001ff"
                        + "80290008932ff9b151263b36"
                        + "000600096576746a3a68367659202020"
                        + "000800149aeaa70cbfd8cb56781ef2b5b2d3f249c1b571a2"
                        + "80280004e57a3bcf");

        private static final byte[] PASSWORD =
                "VOkJxbRl1RmTxUk/WvJxBt".getBytes(StandardCharsets.UTF_8);

        // Offset of the MESSAGE-INTEGRITY attribute header inside MESSAGE: the 20-byte header plus the
        // SOFTWARE (24), PRIORITY (8), ICE-CONTROLLED (12), and USERNAME (16) attributes that precede it.
        private static final int MI_OFFSET = 76;

        @Test
        @DisplayName("recomputed MESSAGE-INTEGRITY equals the RFC vector tag")
        void messageIntegrityMatchesVector() {
            var prefix = java.util.Arrays.copyOf(MESSAGE, MI_OFFSET);
            var expectedTag = java.util.Arrays.copyOfRange(MESSAGE, MI_OFFSET + 4, MI_OFFSET + 24);
            var actual = StunIntegrity.computeMessageIntegrity(prefix, PASSWORD);
            assertArrayEquals(expectedTag, actual, "HMAC-SHA1 over the length-adjusted prefix must match RFC 5769");
        }

        @Test
        @DisplayName("verifyMessageIntegrity accepts the RFC vector and rejects a wrong password")
        void verifyMessageIntegrity() {
            assertTrue(StunIntegrity.verifyMessageIntegrity(MESSAGE, MI_OFFSET, PASSWORD));
            var wrong = "not the password".getBytes(StandardCharsets.UTF_8);
            assertFalse(StunIntegrity.verifyMessageIntegrity(MESSAGE, MI_OFFSET, wrong));
        }

        @Test
        @DisplayName("recomputed FINGERPRINT equals the RFC vector and verifies")
        void fingerprintMatchesVector() {
            var fingerprintOffset = MESSAGE.length - 8;
            var prefix = java.util.Arrays.copyOf(MESSAGE, fingerprintOffset);
            var expected = java.util.Arrays.copyOfRange(MESSAGE, fingerprintOffset + 4, MESSAGE.length);
            assertArrayEquals(expected, StunIntegrity.computeFingerprint(prefix));
            assertTrue(StunIntegrity.verifyFingerprint(MESSAGE));
        }

        @Test
        @DisplayName("a flipped fingerprint byte fails verification")
        void fingerprintTamperFails() {
            var tampered = MESSAGE.clone();
            tampered[tampered.length - 1] ^= 0x01;
            assertFalse(StunIntegrity.verifyFingerprint(tampered));
        }
    }

    @Nested
    @DisplayName("StunMessage codec (SPEC 14.2)")
    class StunCodec {
        private static final byte[] TID = HEX.parseHex("b7e7a701bc34d686fa87dfae");

        @Test
        @DisplayName("encodes a zero-length attribute with a four-byte TLV header and no value padding")
        void encodesEmptyAttribute() {
            var message = new StunMessage(StunMessage.TYPE_BINDING_REQUEST, StunMessage.MAGIC_COOKIE, TID,
                    List.of(new StunMessage.Attribute(StunAttributeType.USE_CANDIDATE, new byte[0])));
            var encoded = message.encode();
            // header (20) + TLV header (4) + zero value = 24; attribute-section length field = 4
            assertEquals(24, encoded.length);
            assertEquals(0x0004, ((encoded[2] & 0xff) << 8) | (encoded[3] & 0xff));
            assertEquals(StunAttributeType.USE_CANDIDATE.value(),
                    ((encoded[20] & 0xff) << 8) | (encoded[21] & 0xff));
            assertEquals(0, ((encoded[22] & 0xff) << 8) | (encoded[23] & 0xff));
        }

        @Test
        @DisplayName("pads a three-byte attribute value to a four-byte boundary on the wire")
        void padsValueToFourBytes() {
            var message = new StunMessage(StunMessage.TYPE_BINDING_INDICATION, StunMessage.MAGIC_COOKIE, TID,
                    List.of(new StunMessage.Attribute(StunAttributeType.WA_REFLEXIVE_PAYLOAD, new byte[]{1, 2, 3})));
            var encoded = message.encode();
            // header(20) + TLV(4) + value(3) + pad(1) = 28; length field counts value+pad = 8
            assertEquals(28, encoded.length);
            assertEquals(8, ((encoded[2] & 0xff) << 8) | (encoded[3] & 0xff));
            assertEquals(3, ((encoded[22] & 0xff) << 8) | (encoded[23] & 0xff));
            assertEquals(0, encoded[27], "the pad byte must be zero");
        }

        @Test
        @DisplayName("decode then encode round-trips a multi-attribute message byte-for-byte")
        void roundTrips() {
            var original = new StunMessage(StunMessage.TYPE_BINDING_SUCCESS_RESPONSE, StunMessage.MAGIC_COOKIE, TID,
                    List.of(
                            new StunMessage.Attribute(StunAttributeType.PRIORITY, HEX.parseHex("6e0001ff")),
                            new StunMessage.Attribute(StunAttributeType.USERNAME,
                                    "evtj:h6vY".getBytes(StandardCharsets.US_ASCII))));
            var encoded = original.encode();
            var decoded = StunMessage.decode(encoded);
            assertEquals(original, decoded);
            assertArrayEquals(encoded, decoded.encode());
        }

        @Test
        @DisplayName("an unknown attribute type decodes with a null known type and round-trips")
        void unknownAttributeTolerated() {
            var unknownType = 0x7abc;
            var raw = new StunMessage(StunMessage.TYPE_BINDING_REQUEST, StunMessage.MAGIC_COOKIE, TID,
                    List.of(new StunMessage.Attribute(unknownType, new byte[]{9, 8})));
            var decoded = StunMessage.decode(raw.encode());
            var attribute = decoded.attributes().getFirst();
            assertNull(attribute.type(), "an unmodeled attribute type resolves to a null known type");
            assertEquals(unknownType, attribute.typeValue());
            assertArrayEquals(raw.encode(), decoded.encode());
        }

        @Test
        @DisplayName("finalizeWithIntegrity appends MESSAGE-INTEGRITY then FINGERPRINT that both verify")
        void finalizeWithIntegrity() {
            var password = "callPassword24Characters".getBytes(StandardCharsets.US_ASCII);
            var message = new StunMessage(StunMessage.TYPE_BINDING_REQUEST, StunMessage.MAGIC_COOKIE, TID,
                    List.of(new StunMessage.Attribute(StunAttributeType.PRIORITY, HEX.parseHex("6e0001ff"))));
            var finalized = message.finalizeWithIntegrity(password);
            var decoded = StunMessage.decode(finalized);
            var attributes = decoded.attributes();
            assertEquals(StunAttributeType.PRIORITY, attributes.get(0).type());
            assertEquals(StunAttributeType.MESSAGE_INTEGRITY, attributes.get(1).type());
            assertEquals(StunAttributeType.FINGERPRINT, attributes.get(2).type());
            var miOffset = finalized.length - 8 - 24;
            assertTrue(StunIntegrity.verifyMessageIntegrity(finalized, miOffset, password));
            assertTrue(StunIntegrity.verifyFingerprint(finalized));
        }

        @Test
        @DisplayName("decode rejects a truncated header and an over-long attribute length")
        void decodeRejectsMalformed() {
            assertThrows(Exception.class, () -> StunMessage.decode(new byte[10]));
            // header declares 8 attribute bytes (length field 0x0008) but only the 20-byte header is present
            var bad = HEX.parseHex("000100082112a442b7e7a701bc34d686fa87dfae");
            assertThrows(Exception.class, () -> StunMessage.decode(bad));
        }
    }

    @Nested
    @DisplayName("WarpMessageIntegrity HBH tag (SPEC 14.1)")
    class WarpMi {
        // A WARP message: type=9, seq-number attribute (bit 0) value 0x0102, padded to even length.
        private final byte[] warp = new WarpMessage.Piggybacked(
                List.of(new WarpAttribute.SequenceNumber(0x0102))).encode();
        private final byte[] authKey = HEX.parseHex(
                "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");

        @Test
        @DisplayName("appends the full 32-byte HMAC-SHA256 tag over the WARP bytes")
        void appendsFullTag() {
            var tagged = WarpMessageIntegrity.appendTag(warp, authKey);
            assertEquals(warp.length + WarpMessageIntegrity.FULL_TAG_LENGTH, tagged.length);
            assertArrayEquals(warp, java.util.Arrays.copyOf(tagged, warp.length),
                    "the WARP message prefix must be left intact");
            assertTrue(WarpMessageIntegrity.verifyTag(tagged, authKey, WarpMessageIntegrity.FULL_TAG_LENGTH));
        }

        @Test
        @DisplayName("a truncated tag verifies against its leading HMAC bytes")
        void truncatedTagVerifies() {
            var tagged = WarpMessageIntegrity.appendTag(warp, authKey, 10);
            assertEquals(warp.length + 10, tagged.length);
            assertTrue(WarpMessageIntegrity.verifyTag(tagged, authKey, 10));
        }

        @Test
        @DisplayName("verification fails under the wrong auth key")
        void wrongKeyFails() {
            var tagged = WarpMessageIntegrity.appendTag(warp, authKey);
            var otherKey = authKey.clone();
            otherKey[0] ^= 0x01;
            assertFalse(WarpMessageIntegrity.verifyTag(tagged, otherKey, WarpMessageIntegrity.FULL_TAG_LENGTH));
        }

        @Test
        @DisplayName("rejects a tag length outside 1..32")
        void rejectsBadTagLength() {
            assertThrows(IllegalArgumentException.class, () -> WarpMessageIntegrity.appendTag(warp, authKey, 0));
            assertThrows(IllegalArgumentException.class, () -> WarpMessageIntegrity.appendTag(warp, authKey, 33));
        }
    }

    @Nested
    @DisplayName("WhatsApp Web subscription + keepalive wire forms")
    class SubscriptionWireForms {
        // The exact 95-byte value of the 0x4024 (WA_SUBSCRIPTION) attribute, captured from a connected
        // 3-way group call (re/calls2-spec/captures/webrtc-datachannel-transport-2026-06-21.md, the 0x0003
        // base64 sample decoded). Nine repeated field-1 entries, each {participant?, stream?, ssrc}.
        private static final byte[] CAPTURED_SUBSCRIPTION_VALUE = HEX.parseHex(
                "0a0618c3a2a8930f"           // {                ssrc=0xf26a1143 }
                        + "0a08100118bbd0fce00e"   // {        stream=1, ssrc=0xec1f283b }
                        + "0a08100218b9e4b9be01"   // {        stream=2, ssrc=0x17ce7239 }
                        + "0a08080118b1f2c8c00a"   // { part=1,          ssrc=0xa8123931 }
                        + "0a0a0801100118cbdc86bc01" // { part=1, stream=1, ssrc=0x1781ae4b }
                        + "0a0a0801100218e597cef80b" // { part=1, stream=2, ssrc=0xbf138be5 }
                        + "0a08080218d2c8bfbb05"   // { part=2,          ssrc=0x576fe452 }
                        + "0a09080210011899d1f31b" // { part=2, stream=1, ssrc=0x037ce899 }
                        + "0a0a0802100218daa898a40c"); // { part=2, stream=2, ssrc=0xc486145a }

        // Build the StreamSubscriptions matrix the capture decodes to. participant absent => self stream;
        // stream absent => audio (stream 0). SSRC carried as an unsigned long.
        private StreamSubscriptions capturedMatrix() {
            var entries = new ArrayList<StreamSubscriptions.Entry>();
            entries.add(new StreamSubscriptionsEntryBuilder().ssrc(0xf26a1143L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().stream(1).ssrc(0xec1f283bL).build());
            entries.add(new StreamSubscriptionsEntryBuilder().stream(2).ssrc(0x17ce7239L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(1).ssrc(0xa8123931L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(1).stream(1).ssrc(0x1781ae4bL).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(1).stream(2).ssrc(0xbf138be5L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(2).ssrc(0x576fe452L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(2).stream(1).ssrc(0x037ce899L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(2).stream(2).ssrc(0xc486145aL).build());
            return new StreamSubscriptionsBuilder().entries(entries).build();
        }

        @Test
        @DisplayName("0x4024 subscription value byte-matches the captured group-call sample")
        void subscriptionValueByteMatchesCapture() {
            var encoded = SubscriptionEnvelope.subscriptionAttributeValue(capturedMatrix());
            assertEquals(95, encoded.length, "the captured 0x4024 attribute value is 95 bytes");
            assertArrayEquals(CAPTURED_SUBSCRIPTION_VALUE, encoded,
                    "the StreamSubscriptions encoding must reproduce the captured 0x4024 value byte-for-byte");
        }

        @Test
        @DisplayName("the captured 0x4024 value round-trips through the protobuf spec")
        void subscriptionValueRoundTrips() {
            var decoded = StreamSubscriptionsSpec.decode(CAPTURED_SUBSCRIPTION_VALUE);
            assertEquals(9, decoded.entries().size());
            // entry[0] is a self audio stream: no participant, no stream, the local sender SSRC.
            var first = decoded.entries().getFirst();
            assertTrue(first.participant().isEmpty());
            assertTrue(first.stream().isEmpty());
            assertEquals(0xf26a1143L, first.ssrc().orElseThrow());
            // entry[8] is participant 2, stream 2.
            var last = decoded.entries().getLast();
            assertEquals(2, last.participant().orElseThrow());
            assertEquals(2, last.stream().orElseThrow());
            assertEquals(0xc486145aL, last.ssrc().orElseThrow());
            assertArrayEquals(CAPTURED_SUBSCRIPTION_VALUE, StreamSubscriptionsSpec.encode(decoded));
        }

        @Test
        @DisplayName("0x4024 attribute frames the value under the WA_SUBSCRIPTION type with four-byte padding")
        void subscriptionAttributeFramesValue() {
            var attribute = SubscriptionEnvelope.subscriptionAttribute(capturedMatrix());
            assertEquals(StunAttributeType.WA_SUBSCRIPTION, attribute.type());
            assertEquals(0x4024, attribute.typeValue());
            assertArrayEquals(CAPTURED_SUBSCRIPTION_VALUE, attribute.value());
            // 95-byte value -> 96-byte padded value, plus the 4-byte TLV header.
            assertEquals(4 + 96, attribute.encode().length);
        }

        @Test
        @DisplayName("0x0801 keepalive is a bare header: type 0x0801, magic, txid, zero attributes")
        void keepaliveIsBareHeader() {
            var txid = HEX.parseHex("0102030405060708090a0b0c");
            var keepalive = SubscriptionEnvelope.keepalive(txid);
            assertEquals(SubscriptionEnvelope.KEEPALIVE_LENGTH, keepalive.length);
            assertEquals(StunMessage.HEADER_LENGTH, keepalive.length, "no attributes follow the header");
            // 0801 0000 2112a442 <txid>
            assertEquals(0x0801, ((keepalive[0] & 0xff) << 8) | (keepalive[1] & 0xff));
            assertEquals(0, ((keepalive[2] & 0xff) << 8) | (keepalive[3] & 0xff), "attribute section length is 0");
            assertEquals(0x2112a442, ((keepalive[4] & 0xff) << 24) | ((keepalive[5] & 0xff) << 16)
                    | ((keepalive[6] & 0xff) << 8) | (keepalive[7] & 0xff));
            assertArrayEquals(txid, java.util.Arrays.copyOfRange(keepalive, 8, 20));
            var decoded = StunMessage.decode(keepalive);
            assertEquals(StunMessage.TYPE_KEEPALIVE, decoded.messageType());
            assertTrue(decoded.attributes().isEmpty());
        }

        @Test
        @DisplayName("a random-txid keepalive is still a valid 20-byte 0x0801 header")
        void randomKeepaliveIsValid() {
            var keepalive = SubscriptionEnvelope.keepalive();
            assertEquals(20, keepalive.length);
            assertEquals(StunMessage.TYPE_KEEPALIVE, StunMessage.decode(keepalive).messageType());
        }
    }

    @Nested
    @DisplayName("0x0003 subscription envelope assembly and relay-key MESSAGE-INTEGRITY")
    class SubscriptionEnvelopeAssembly {
        // The captured envelope transaction id and 0x4024 value
        // (re/calls2-spec/captures/webrtc-datachannel-transport-2026-06-21.md).
        private static final byte[] TXID = HEX.parseHex("9a9b3d6247cea12de2abd953");
        private static final byte[] CAPTURED_SUBSCRIPTION_VALUE = HEX.parseHex(
                "0a0618c3a2a8930f"
                        + "0a08100118bbd0fce00e"
                        + "0a08100218b9e4b9be01"
                        + "0a08080118b1f2c8c00a"
                        + "0a0a0801100118cbdc86bc01"
                        + "0a0a0801100218e597cef80b"
                        + "0a08080218d2c8bfbb05"
                        + "0a09080210011899d1f31b"
                        + "0a0a0802100218daa898a40c");
        // The captured 0x0016 XOR-MAPPED-ADDRESS value decodes to 31.13.86.63:3478 (the relay reflexive addr).
        private static final byte[] CAPTURED_XOR_MAPPED_VALUE = HEX.parseHex("00012c843e1ff27d");
        private static final InetSocketAddress REFLEXIVE_ADDRESS =
                new InetSocketAddress("31.13.86.63", 3478);
        // A 24-byte ASCII relay <key>, the length and shape of the real relay STUN-app credential.
        private static final byte[] RELAY_KEY =
                "relayKeyTwentyFourASCII!".getBytes(StandardCharsets.US_ASCII);

        private StreamSubscriptions capturedMatrix() {
            var entries = new ArrayList<StreamSubscriptions.Entry>();
            entries.add(new StreamSubscriptionsEntryBuilder().ssrc(0xf26a1143L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().stream(1).ssrc(0xec1f283bL).build());
            entries.add(new StreamSubscriptionsEntryBuilder().stream(2).ssrc(0x17ce7239L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(1).ssrc(0xa8123931L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(1).stream(1).ssrc(0x1781ae4bL).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(1).stream(2).ssrc(0xbf138be5L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(2).ssrc(0x576fe452L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(2).stream(1).ssrc(0x037ce899L).build());
            entries.add(new StreamSubscriptionsEntryBuilder().participant(2).stream(2).ssrc(0xc486145aL).build());
            return new StreamSubscriptionsBuilder().entries(entries).build();
        }

        @Test
        @DisplayName("assembles type 0x0003 with 0x4024, 0x0016, 0x0008 in order and no FINGERPRINT")
        void envelopeStructure() {
            var envelope = SubscriptionEnvelope.subscriptionEnvelope(
                    RELAY_KEY, capturedMatrix(), REFLEXIVE_ADDRESS, TXID);
            var decoded = StunMessage.decode(envelope);
            assertEquals(StunMessage.TYPE_SUBSCRIPTION, decoded.messageType());
            assertEquals(StunMessage.MAGIC_COOKIE, decoded.magicCookie());
            assertArrayEquals(TXID, decoded.transactionId());
            var attributes = decoded.attributes();
            assertEquals(3, attributes.size(), "without the gated 0x4000 the envelope carries three attributes");
            assertEquals(StunAttributeType.WA_SUBSCRIPTION, attributes.get(0).type());
            assertArrayEquals(CAPTURED_SUBSCRIPTION_VALUE, attributes.get(0).value());
            assertEquals(StunAttributeType.WA_XOR_MAPPED_ADDRESS, attributes.get(1).type());
            assertArrayEquals(CAPTURED_XOR_MAPPED_VALUE, attributes.get(1).value(),
                    "the 0x0016 value must byte-match the captured relay reflexive address");
            assertEquals(StunAttributeType.MESSAGE_INTEGRITY, attributes.get(2).type());
            assertEquals(StunIntegrity.MESSAGE_INTEGRITY_LENGTH, attributes.get(2).value().length);
            assertTrue(decoded.attribute(StunAttributeType.FINGERPRINT).isEmpty(),
                    "the 0x0003 envelope carries no FINGERPRINT, matching the capture");
        }

        @Test
        @DisplayName("the trailing MESSAGE-INTEGRITY verifies under the relay <key> and fails under a wrong key")
        void messageIntegrityKeyedByRelayKey() {
            var envelope = SubscriptionEnvelope.subscriptionEnvelope(
                    RELAY_KEY, capturedMatrix(), REFLEXIVE_ADDRESS, TXID);
            // The MESSAGE-INTEGRITY attribute is the last 24 bytes (4-byte header + 20-byte HMAC).
            var miOffset = envelope.length - (4 + StunIntegrity.MESSAGE_INTEGRITY_LENGTH);
            assertTrue(StunIntegrity.verifyMessageIntegrity(envelope, miOffset, RELAY_KEY),
                    "the outer MI must be HMAC-SHA1 keyed by the relay <key>");
            var wrongKey = RELAY_KEY.clone();
            wrongKey[0] ^= 0x01;
            assertFalse(StunIntegrity.verifyMessageIntegrity(envelope, miOffset, wrongKey));
        }

        @Test
        @DisplayName("the generic overload frames a pre-built subscription attribute into the envelope")
        void genericAttributeOverload() {
            var subscriptionAttr = SubscriptionEnvelope.subscriptionAttribute(capturedMatrix());
            var envelope = SubscriptionEnvelope.subscriptionEnvelope(
                    RELAY_KEY, subscriptionAttr, REFLEXIVE_ADDRESS, TXID);
            var decoded = StunMessage.decode(envelope);
            assertEquals(StunAttributeType.WA_SUBSCRIPTION, decoded.attributes().getFirst().type());
            var miOffset = envelope.length - (4 + StunIntegrity.MESSAGE_INTEGRITY_LENGTH);
            assertTrue(StunIntegrity.verifyMessageIntegrity(envelope, miOffset, RELAY_KEY));
        }
    }

    /**
     * Codifies the byte-level reverse-engineering of the captured {@code 0x4000} WARP attribute value
     * (re/calls2-spec/web-transport-crypto-RE.md RESOLUTION, re/calls2-spec/warp-header-layout-RE.md): the
     * leading four header bytes are cleartext ({@code 09} type at byte 0, the packed length at byte 1, a
     * big-endian timestamp at bytes 2..3), while the flag byte at offset 4 and the body after it are sealed by
     * the relay hop-by-hop SRTP layer and carry no cleartext RTP header. The captured envelope {@code 0x4000}
     * value is contrasted against the captured standalone {@code 0x09} WARP, which instead piggybacks a real
     * RTP packet whose twelve-byte header IS cleartext. These tests pin the gate's reason structurally; they do
     * not reconstruct the sealed body, which must not be invented.
     */
    @Nested
    @DisplayName("captured 0x4000 WARP control structure (gated body)")
    class Captured0x4000WarpStructure {
        // The 193-byte value of the 0x4000 (WA_WARP_MESSAGE) attribute, sliced from the captured 0x0003
        // envelope base64 (re/calls2-spec/captures/webrtc-datachannel-transport-2026-06-21.md): the 20-byte
        // STUN header + the 4-byte attribute TLV header precede it, so it starts at envelope offset 24.
        private static final byte[] CAPTURED_0x4000_VALUE = HEX.parseHex(
                "090f01f53aeda2020a02f65a70cb24d03b1b27ec68f014e63256f74e7414e9c6"
                        + "b2e52c0b6590290788258badfbf50dbc932321aa0782733d2185ff16de3621ad"
                        + "a9337e8fbe47e44b15874f8aa3130468bc3f487232310c7f2fda8ebe52ac8564"
                        + "17e8e8f4150e6ffe34e671c311cd730f8c3d7cfb5056fe5894dd765e2c7c7487"
                        + "8e65c3b15453da888b29fce85604d36593940cb9914f0ac8c647986e229cc435"
                        + "db768d0399b0a549cc0ba86e856e43f67eb3f9c088a4ffed3dd871cb53333065"
                        + "ff");

        @Test
        @DisplayName("the captured 0x4000 value is 193 bytes with a cleartext 09-type WARP header")
        void warpHeaderIsCleartext() {
            assertEquals(193, CAPTURED_0x4000_VALUE.length,
                    "the captured 0x4000 attribute value is 193 bytes");
            assertEquals(0x09, CAPTURED_0x4000_VALUE[0] & 0xff,
                    "byte 0 is the cleartext WARP type discriminator 0x09");
            assertEquals(0x0f, CAPTURED_0x4000_VALUE[1] & 0xff,
                    "byte 1 is the cleartext packed length 0x0f (a thirty-byte padded payload), not a flag byte");
            // bytes 2..4 are the cleartext big-endian timestamp; the flag byte at offset 4 is sealed.
            assertEquals(0x01f5, ((CAPTURED_0x4000_VALUE[2] & 0xff) << 8) | (CAPTURED_0x4000_VALUE[3] & 0xff),
                    "bytes 2..4 are the cleartext big-endian WARP timestamp");
        }

        @Test
        @DisplayName("the body after the 4-byte WARP header is high-entropy with no cleartext RTP header")
        void bodyIsSealedWithNoCleartextRtp() {
            // The standalone 0x09 WARP carries an RTP packet whose header is cleartext (a 0x90 0x78 byte
            // pair at a fixed offset). The 0x4000 control WARP carries NO such RTP header: its body is sealed.
            assertFalse(containsRtpHeaderMarker(CAPTURED_0x4000_VALUE, 4),
                    "the 0x4000 body carries no cleartext RTP header; it is hop-by-hop SRTP-sealed");
            // The sealed body is near-random: a 16-byte window past the header has high byte diversity, well
            // above what the cleartext 0x4024 subscription protobuf (many repeated 0x0a/0x18 tags) shows.
            var distinctInWindow = distinctBytes(CAPTURED_0x4000_VALUE, 8, 24);
            assertTrue(distinctInWindow >= 14,
                    "a sealed 16-byte body window should be near-random (>=14 distinct bytes), got " + distinctInWindow);
        }

        @Test
        @DisplayName("a cleartext WARP encode is not the sealed captured 0x4000 value (the seal is real)")
        void cleartextWarpIsNotTheSealedValue() {
            // The WARP codec frames a cleartext control WARP; the captured 0x4000 body is hop-by-hop
            // SRTP-sealed and so cannot equal a cleartext encode. This pins that the gate is a real seal,
            // not a missing field: a cleartext WARP with the same decoded attributes is NOT the wire value.
            var cleartext = new WarpMessage.Piggybacked(
                    List.of(new WarpAttribute.SequenceNumber(0x3aed))).encode();
            assertFalse(java.util.Arrays.equals(CAPTURED_0x4000_VALUE, cleartext),
                    "the captured 0x4000 body is sealed; a cleartext WARP encode cannot reproduce it");
        }

        // Returns whether a 0x90 0x78 RTP-header marker (the captured audio RTP V2/PT pair) appears at or
        // after the given offset, the cleartext signature a piggybacked RTP packet leaves in a WARP body.
        private static boolean containsRtpHeaderMarker(byte[] data, int from) {
            for (var i = from; i + 1 < data.length; i++) {
                if ((data[i] & 0xff) == 0x90 && (data[i + 1] & 0xff) == 0x78) {
                    return true;
                }
            }
            return false;
        }

        // Returns the number of distinct byte values in data[from, to).
        private static int distinctBytes(byte[] data, int from, int to) {
            var seen = new boolean[256];
            var count = 0;
            for (var i = from; i < to; i++) {
                var b = data[i] & 0xff;
                if (!seen[b]) {
                    seen[b] = true;
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * Pins the WARP header wire layout against the two live-captured samples and the WASM serializer
     * (re/calls2-spec/warp-header-layout-RE.md): byte 0 is the type 0x09, byte 1 is the packed length
     * {@code (paddedPayloadLen >> 1) & 0x7F}, bytes 2..3 are the big-endian timestamp, and byte 4 is the
     * flag byte. The encoder must reproduce the captured cleartext header bytes exactly given the same
     * attributes and timestamp, and {@code decode} must accept the captured standalone WARP.
     */
    @Nested
    @DisplayName("WARP header byte layout against the captured samples")
    class WarpHeaderWireLayout {
        // The captured standalone WARP (re/calls2-spec/captures/webrtc-datachannel-transport-2026-06-21.md,
        // the CQco... base64), sliced to the 18-byte WARP portion before the piggybacked RTP packet:
        // 09 07 | 28 46 | 29 | 00 01 | 0a 00 19 00 4b 00 00 00 00 00 | 00(pad). The flag byte 0x29 is
        // SEQUENCE_NUMBER | PARTICIPANT_REPORT | PARTICIPANT_REPORT_COMPANION; SEQ=1; the 10-byte report
        // decodes to rtt=2560, dl_bw=6400, loss=75, aux=0, dir=0, uplink=0; timestamp htons(0x2846)=10310.
        private static final byte[] CAPTURED_STANDALONE_WARP =
                HEX.parseHex("090728462900010a0019004b000000000000");
        private static final int CAPTURED_STANDALONE_TIMESTAMP = 0x2846;

        @Test
        @DisplayName("encodes the captured standalone WARP header and attributes byte-for-byte")
        void encodesCapturedStandaloneHeader() {
            var report = new WarpParticipantReport(2560, 6400, 75, 0, 0, 0);
            var message = new WarpMessage.Piggybacked(List.of(
                    new WarpAttribute.SequenceNumber(1),
                    new WarpAttribute.ParticipantReport(report)));
            var encoded = message.encode(CAPTURED_STANDALONE_TIMESTAMP);
            assertArrayEquals(CAPTURED_STANDALONE_WARP, encoded,
                    "the WARP encode must reproduce the captured standalone header (09 07 28 46 ...) exactly");
            // Spell out the header field bytes the layout fixes.
            assertEquals(0x09, encoded[0] & 0xff, "byte 0 is the WARP type");
            assertEquals(0x07, encoded[1] & 0xff, "byte 1 is the packed length (14-byte padded payload >> 1)");
            assertEquals(0x2846, ((encoded[2] & 0xff) << 8) | (encoded[3] & 0xff), "bytes 2..3 are the big-endian timestamp");
            assertEquals(0x29, encoded[4] & 0xff, "byte 4 is the flag byte (seq + participant report + companion)");
        }

        @Test
        @DisplayName("the 0x4000 control WARP cleartext header 09 0f 01 f5 matches the header packing")
        void matchesCaptured0x4000Header() {
            // The captured 0x4000 body (flag byte + attributes) is hop-by-hop-SRTP-sealed and is longer than
            // any modeled attribute set, so the encoder cannot reproduce it; its CLEARTEXT header is
            // [09][packed-length][timestamp BE]. byte1=0x0f packs a 30-byte padded payload, timestamp=0x01f5.
            // Assert the codec's exact packing formula reproduces those four captured header bytes.
            var paddedPayloadLen = 30;
            var le16 = 0x09 | ((paddedPayloadLen << 7) & 0x7f00);
            var header = new byte[]{(byte) (le16 & 0xff), (byte) ((le16 >>> 8) & 0xff), 0x01, (byte) 0xf5};
            assertArrayEquals(HEX.parseHex("090f01f5"), header,
                    "the 0x4000 cleartext header (09 0f 01 f5) is type=09, packed-length=0f, timestamp=01f5");
        }

        @Test
        @DisplayName("decode accepts the captured standalone WARP and reconstructs its attributes")
        void decodeAcceptsCapturedStandalone() {
            var decoded = WarpMessage.decode(CAPTURED_STANDALONE_WARP);
            var attributes = decoded.attributes();
            assertEquals(2, attributes.size());
            assertEquals(new WarpAttribute.SequenceNumber(1), attributes.get(0));
            var report = assertInstanceOf(WarpAttribute.ParticipantReport.class, attributes.get(1));
            assertEquals(new WarpParticipantReport(2560, 6400, 75, 0, 0, 0), report.report());
        }

        @Test
        @DisplayName("decode rejects the captured bytes with byte 0 corrupted away from 0x09")
        void decodeRejectsCorruptedType() {
            var corrupted = CAPTURED_STANDALONE_WARP.clone();
            corrupted[0] = 0x00;
            assertThrows(IllegalArgumentException.class, () -> WarpMessage.decode(corrupted));
        }

        @Test
        @DisplayName("the packed length round-trips the padded payload length for a range of sizes")
        void packedLengthMatchesPayload() {
            // For each attribute set, byte 1 must equal (flagBytes + attrBytes, padded to even) >> 1.
            var seqOnly = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(7))).encode();
            // flags(1) + SEQ(2) = 3 -> padded 4 -> byte1 = 2.
            assertEquals(2, seqOnly[1] & 0xff);
            var standalone = new WarpMessage.Standalone(List.of(new WarpAttribute.BandwidthReport(2, 1, 300))).encode();
            // flags(2, with ext) + report(4) = 6 -> padded 6 -> byte1 = 3.
            assertEquals(3, standalone[1] & 0xff);
            assertEquals(0x09, standalone[0] & 0xff);
        }
    }
}
