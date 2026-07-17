package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.calls.crypto.CallE2eKeyDerivation;
import com.github.auties00.cobalt.calls.util.TimerHeap;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptionsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptionsSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxVidSubscriptionInfo;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxVidSubscriptionInfoBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptions;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionsBuilder;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SenderSubscriptionsSpec;
import com.github.auties00.cobalt.wire.linked.call.datachannel.StreamDescriptor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.calls.transport.srtp.LiveHbhSrtpRelay;
import com.github.auties00.cobalt.calls.transport.srtp.SfuKeyDeriver;
import com.github.auties00.cobalt.calls.transport.srtp.SrtpCryptoSuite;
import com.github.auties00.cobalt.calls.transport.stun.StunAttributeType;
import com.github.auties00.cobalt.calls.transport.stun.StunIntegrity;
import com.github.auties00.cobalt.calls.transport.stun.StunMessage;
import com.github.auties00.cobalt.calls.transport.subscription.LiveSubscriptionPublisher;
import com.github.auties00.cobalt.calls.transport.subscription.RtcpRxSubscriptionEntry;
import com.github.auties00.cobalt.calls.transport.subscription.RtcpRxSubscriptionTable;
import com.github.auties00.cobalt.calls.transport.subscription.StreamLayout;
import com.github.auties00.cobalt.calls.transport.subscription.SubscriptionStunAttribute;
import com.github.auties00.cobalt.calls.transport.warp.WarpAttribute;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessage;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessageIntegrity;

/**
 * Independent adversarial verification of the calls {@code net.transport} wire codecs against SPEC 14
 * and the live captures (transport.json, group-sfu.json). Every expected value here is computed in this
 * test from the wire grammar or from a raw JCA primitive, never copied from the implementation's own
 * output, so a byte-layout, length-adjustment, or label regression in the production code is caught.
 *
 * <p>This suite is the second, independent reviewer of {@link TransportWireFormatTest} and
 * {@link WarpMessageCodecTest}; where those derive STUN/WARP vectors from RFC 5769 and the spec grammar,
 * this one re-derives the STUN MESSAGE-INTEGRITY and FINGERPRINT from a hand-run HMAC-SHA1/CRC32, the
 * SFU KDF labels from a hand-run RFC 5869 HKDF, the WARP packed-length field from the recovered
 * {@code (attr_bytes & 0xfe) << 7} formula, and round-trips the reused subscription protobufs through
 * the proprietary {@code 0x4025}/{@code 0x4021} STUN attributes. The HBH-SRTP libsrtp round-trip is
 * guarded so it is skipped, not failed, when the native library is absent on the build host.
 */
@DisplayName("calls transport wire (adversarial)")
class TransportWireAdversarialTest {
    private static final HexFormat HEX = HexFormat.of();

    @Nested
    @DisplayName("STUN integrity from an independent HMAC-SHA1 and CRC32 (SPEC 14.2)")
    class StunIntegrityKnownAnswer {
        // A hand-built STUN Binding Request with one PRIORITY attribute, before the integrity
        // attributes are appended. Header: type 0x0001, len patched by encode(), cookie 0x2112A442,
        // 12-byte transaction id, then PRIORITY (0x0024) length 4 value 6e0001ff.
        private final byte[] tid = HEX.parseHex("0102030405060708090a0b0c");
        private final byte[] password = "VOkJxbRl1RmTxUk/WvJxBt".getBytes(StandardCharsets.US_ASCII);
        private final StunMessage base = new StunMessage(
                StunMessage.TYPE_BINDING_REQUEST, StunMessage.MAGIC_COOKIE, tid,
                List.of(new StunMessage.Attribute(StunAttributeType.PRIORITY, HEX.parseHex("6e0001ff"))));

        @Test
        @DisplayName("MESSAGE-INTEGRITY equals HMAC-SHA1 over the prefix with the length spanning the MI attribute")
        void messageIntegrityKnownAnswer() throws Exception {
            var prefix = base.encode();
            // STUN requires the length field to count through the 24-byte MESSAGE-INTEGRITY attribute
            // even though it is not yet present. The attribute section is the current attributes (8 bytes:
            // PRIORITY TLV) plus 24 = 32 = 0x20.
            var adjusted = prefix.clone();
            adjusted[2] = 0x00;
            adjusted[3] = (byte) (prefix.length - StunMessage.HEADER_LENGTH + 24);
            var mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(password, "HmacSHA1"));
            var expected = mac.doFinal(adjusted);

            assertArrayEquals(expected, StunIntegrity.computeMessageIntegrity(prefix, password),
                    "MESSAGE-INTEGRITY must be HMAC-SHA1 over the length-spanned prefix");
        }

        @Test
        @DisplayName("FINGERPRINT equals CRC32 over the prefix spanning the fingerprint attribute, XOR 0x5354554E")
        void fingerprintKnownAnswer() {
            // Build the prefix that the FINGERPRINT covers: base + MESSAGE-INTEGRITY attribute.
            var withMi = base.encode();
            var mi = StunIntegrity.computeMessageIntegrity(withMi, password);
            var prefix = appendAttribute(withMi, StunAttributeType.MESSAGE_INTEGRITY.value(), mi);

            var adjusted = prefix.clone();
            var sectionThroughFingerprint = prefix.length - StunMessage.HEADER_LENGTH + 8;
            adjusted[2] = (byte) (sectionThroughFingerprint >>> 8);
            adjusted[3] = (byte) sectionThroughFingerprint;
            var crc = new CRC32();
            crc.update(adjusted);
            var value = (crc.getValue() ^ 0x5354554EL) & 0xFFFFFFFFL;
            var expected = new byte[]{
                    (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};

            assertArrayEquals(expected, StunIntegrity.computeFingerprint(prefix),
                    "FINGERPRINT must be CRC32 of the length-spanned prefix XOR ASCII 'STUN'");
        }

        @Test
        @DisplayName("finalizeWithIntegrity produces a message both helpers verify and the FINGERPRINT covers the MI")
        void finalizeVerifies() {
            var finalized = base.finalizeWithIntegrity(password);
            var miOffset = finalized.length - 8 - 24;
            assertTrue(StunIntegrity.verifyMessageIntegrity(finalized, miOffset, password));
            assertTrue(StunIntegrity.verifyFingerprint(finalized));
            // Flipping any MI byte must break the fingerprint too, since the fingerprint covers the MI.
            var tampered = finalized.clone();
            tampered[miOffset + 4] ^= 0x01;
            assertFalse(StunIntegrity.verifyFingerprint(tampered));
        }

        // Mirror of StunMessage.appendAttribute: append one TLV and patch the header length field.
        private byte[] appendAttribute(byte[] message, int type, byte[] value) {
            var out = Arrays.copyOf(message, message.length + 4 + value.length);
            var cursor = message.length;
            out[cursor] = (byte) (type >>> 8);
            out[cursor + 1] = (byte) type;
            out[cursor + 2] = (byte) (value.length >>> 8);
            out[cursor + 3] = (byte) value.length;
            System.arraycopy(value, 0, out, cursor + 4, value.length);
            var section = out.length - StunMessage.HEADER_LENGTH;
            out[2] = (byte) (section >>> 8);
            out[3] = (byte) section;
            return out;
        }
    }

    @Nested
    @DisplayName("SfuKeyDeriver HKDF labels (SPEC 14.4 / 7.2)")
    class SfuKdf {
        // RFC 5869 HKDF-SHA256 run by hand against the recovered wa_sfu_kdf labels, zero 32-byte salt.
        private final byte[] baseSecret = HEX.parseHex(
                "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f");
        // A 30-byte relay <hbh_key> (14-byte salt secret + 16-byte key secret), the warp-auth chain input.
        private final byte[] hbhKey = HEX.parseHex(
                "404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d");

        @Test
        @DisplayName("the CERT_FINGERPRINT_HMAC domain expands to a flat HKDF under its exact ASCII label")
        void certLabelMatchesRfc5869() throws Exception {
            var label = "data-channel cert fingerprint hmac";
            var expected = hkdfSha256(baseSecret, new byte[32],
                    label.getBytes(StandardCharsets.US_ASCII), SfuKeyDeriver.DERIVED_KEY_LENGTH);
            assertArrayEquals(expected, SfuKeyDeriver.derive(baseSecret, SfuKeyDeriver.Domain.CERT_FINGERPRINT_HMAC),
                    "CERT_FINGERPRINT_HMAC must use its label with a zero salt");
        }

        @Test
        @DisplayName("the warp auth key is the TWO-STEP chained HKDF over the <hbh_key> split, output 32")
        void warpAuthKeyIsChained() throws Exception {
            // Hand-run the recovered chain: 'warp auth salt' over the 14-byte salt secret (zero salt, L=32),
            // then 'warp auth key' over the 16-byte key secret keyed by that chain salt (L=32).
            var saltSecret = Arrays.copyOfRange(hbhKey, 0, 14);
            var keySecret = Arrays.copyOfRange(hbhKey, 14, 30);
            var chainSalt = hkdfSha256(saltSecret, new byte[32],
                    "warp auth salt".getBytes(StandardCharsets.US_ASCII), 32);
            var expected = hkdfSha256(keySecret, chainSalt,
                    "warp auth key".getBytes(StandardCharsets.US_ASCII), 32);
            var actual = CallE2eKeyDerivation.deriveWarpAuthKey(hbhKey);
            assertEquals(CallE2eKeyDerivation.WARP_AUTH_KEY_LENGTH, actual.length, "warp auth key is 32 bytes");
            assertArrayEquals(expected, actual,
                    "warp auth key must be the chained 'warp auth salt' -> 'warp auth key' derivation");
        }

        @Test
        @DisplayName("the chained warp auth key differs from a flat single-step HKDF of the same <hbh_key>")
        void warpAuthKeyIsNotFlat() throws Exception {
            // The pre-fix bug derived a flat HKDF(hbhKey, zero, "warp auth key", 32); prove the chain differs.
            var flat = hkdfSha256(hbhKey, new byte[32],
                    "warp auth key".getBytes(StandardCharsets.US_ASCII), 32);
            assertFalse(Arrays.equals(flat, CallE2eKeyDerivation.deriveWarpAuthKey(hbhKey)),
                    "the two-step chain must not collapse to the flat single zero-salt HKDF");
        }

        @Test
        @DisplayName("the chained WARP-auth key keys the WARP HBH MI tag end to end")
        void warpAuthKeyDrivesWarpMi() {
            // The chained deriver output is exactly the key WarpMessageIntegrity consumes.
            var key = CallE2eKeyDerivation.deriveWarpAuthKey(hbhKey);
            var warp = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(7))).encode();
            var tagged = WarpMessageIntegrity.appendTag(warp, key);
            assertTrue(WarpMessageIntegrity.verifyTag(tagged, key, WarpMessageIntegrity.FULL_TAG_LENGTH));
        }

        // Independent RFC 5869 extract-then-expand on raw HmacSHA256, not VoipCryptoNative.
        private byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) throws Exception {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt.length == 0 ? new byte[mac.getMacLength()] : salt, "HmacSHA256"));
            var prk = mac.doFinal(ikm);
            var out = new byte[length];
            var t = new byte[0];
            var pos = 0;
            var counter = 1;
            while (pos < length) {
                var prkMac = Mac.getInstance("HmacSHA256");
                prkMac.init(new SecretKeySpec(prk, "HmacSHA256"));
                prkMac.update(t);
                prkMac.update(info);
                prkMac.update((byte) counter);
                t = prkMac.doFinal();
                var n = Math.min(t.length, length - pos);
                System.arraycopy(t, 0, out, pos, n);
                pos += n;
                counter++;
            }
            return out;
        }
    }

    @Nested
    @DisplayName("WARP packed-length field (SPEC 14.1)")
    class WarpPackedLength {
        @ParameterizedTest
        @CsvSource({
                // attr value bytes per attribute -> padded payload bytes (flag byte + attr, padded to even)
                "1,2",   // VideoEncoding: flags(1) + 1 = 2 (even)
                "2,4",   // SequenceNumber: flags(1) + 2 = 3 -> pad to 4
                "4,6",   // SenderBwa: flags(1) + 4 = 5 -> pad to 6
        })
        @DisplayName("byte 1 packs (paddedPayloadLen >> 1) and byte 0 is the type")
        void packedLengthFormula(int rawAttrBytes, int paddedPayloadBytes) {
            WarpAttribute attr = switch (rawAttrBytes) {
                case 1 -> new WarpAttribute.VideoEncoding(0x03);
                case 2 -> new WarpAttribute.SequenceNumber(0x1234);
                case 4 -> new WarpAttribute.SenderBandwidthAllocation(0x11223344);
                default -> throw new IllegalArgumentException();
            };
            var bytes = new WarpMessage.Piggybacked(List.of(attr)).encode();
            assertEquals(WarpMessage.WARP_TYPE, bytes[0] & 0xff, "byte 0 is the WARP type 0x09");
            assertEquals((paddedPayloadBytes >>> 1) & 0x7f, bytes[1] & 0xff,
                    "byte 1 packs the padded payload length over two");
        }

        @Test
        @DisplayName("byte 1 round-trips the padded payload length back as (byte1 & 0x7F) * 2")
        void packedLengthDecodes() {
            // flags(1) + Sequence(2) + SenderBwa(4) = 7 payload bytes -> padded to 8, no extension byte.
            var bytes = new WarpMessage.Piggybacked(List.of(
                    new WarpAttribute.SequenceNumber(1),
                    new WarpAttribute.SenderBandwidthAllocation(2))).encode();
            assertEquals(8, (bytes[1] & 0x7f) * 2, "byte 1 encodes paddedPayloadLen/2 in its low 7 bits");
        }

        @Test
        @DisplayName("a single small attribute produces an even payload length (SPEC 14.1)")
        void totalLengthIsEven() {
            // SPEC 14.1 and the recovered serializer pad the PAYLOAD (flag byte plus attributes) to even.
            // The five-byte header already includes the flag byte at offset 4, so a 1-byte VideoEncoding
            // payload (flags 1 + attr 1 = 2, even) yields a 6-byte total with no pad, and a 2-byte
            // SequenceNumber payload (flags 1 + attr 2 = 3 -> padded to 4) yields an 8-byte total with one
            // pad byte after the attribute.
            var oneByte = new WarpMessage.Piggybacked(List.of(new WarpAttribute.VideoEncoding(0x03))).encode();
            var twoByte = new WarpMessage.Piggybacked(List.of(new WarpAttribute.SequenceNumber(0x1234))).encode();
            assertEquals(6, oneByte.length, "header(5) + attr(1); payload flags(1)+attr(1)=2 is even, no pad");
            assertEquals(8, twoByte.length, "header(5) + attr(2) + pad(1); payload flags(1)+attr(2)=3 -> padded 4");
        }
    }

    @Nested
    @DisplayName("subscription protobufs in proprietary STUN attributes (SPEC 14.3)")
    class Subscriptions {
        @Test
        @DisplayName("a SenderSubscriptions round-trips through the 0x4025 attribute value verbatim")
        void senderRoundTrip() {
            var sender = new SenderSubscriptionsBuilder().subscriptions(List.of()).build();
            var attribute = new SubscriptionStunAttribute(
                    SubscriptionStunAttribute.SENDER_SUBSCRIPTIONS_TYPE, SenderSubscriptionsSpec.encode(sender));
            assertEquals(0x4025, attribute.attributeType());
            assertEquals(sender, SenderSubscriptionsSpec.decode(attribute.value()));
        }

        @Test
        @DisplayName("an RxSubscriptions round-trips through the 0x4021 attribute value verbatim")
        void receiverRoundTrip() {
            var rx = new RxSubscriptionsBuilder()
                    .vidRxPids(List.of(1, 2, 3))
                    .vidSubscriptions(List.of(vidInfo(1, RxVidSubscriptionInfo.VideoQuality.HIGH),
                            vidInfo(2, RxVidSubscriptionInfo.VideoQuality.LOW)))
                    .build();
            var attribute = new SubscriptionStunAttribute(
                    SubscriptionStunAttribute.RECEIVER_SUBSCRIPTION_TYPE, RxSubscriptionsSpec.encode(rx));
            assertEquals(0x4021, attribute.attributeType());
            assertEquals(rx, RxSubscriptionsSpec.decode(attribute.value()));
        }

        @Test
        @DisplayName("the subscription protobuf survives embedding in a STUN binding request and parsing it back")
        void embeddedInStunMessage() {
            var rx = new RxSubscriptionsBuilder().vidRxPids(List.of(9, 8, 7)).build();
            var payload = RxSubscriptionsSpec.encode(rx);
            var tid = HEX.parseHex("0102030405060708090a0b0c");
            var message = new StunMessage(StunMessage.TYPE_BINDING_REQUEST, StunMessage.MAGIC_COOKIE, tid,
                    List.of(new StunMessage.Attribute(
                            StunAttributeType.WA_RECEIVER_SUBSCRIPTION, payload)));

            var decoded = StunMessage.decode(message.encode());
            var attribute = decoded.attribute(StunAttributeType.WA_RECEIVER_SUBSCRIPTION).orElseThrow();
            // The TLV length field reports the unpadded protobuf length; the value parses back to the proto.
            assertArrayEquals(payload, attribute.value());
            assertEquals(rx, RxSubscriptionsSpec.decode(attribute.value()));
        }

        @Test
        @DisplayName("paddedLength rounds the value up to a four-byte boundary without mutating the value")
        void paddedLengthRoundsUp() {
            var three = new SubscriptionStunAttribute(0x4021, new byte[]{1, 2, 3});
            assertEquals(4, three.paddedLength());
            assertEquals(3, three.value().length, "the stored value keeps its unpadded length");
            var four = new SubscriptionStunAttribute(0x4021, new byte[]{1, 2, 3, 4});
            assertEquals(4, four.paddedLength());
            var five = new SubscriptionStunAttribute(0x4021, new byte[]{1, 2, 3, 4, 5});
            assertEquals(8, five.paddedLength());
        }

        @Test
        @DisplayName("buildStreamDescriptors emits one media+FEC+NACK triple for audio and the app-data single")
        void streamDescriptorsFromLayout() {
            var publisher = newPublisher();
            try {
                var layout = new StreamLayout(0x1111, StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC,
                        0x2222, StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC, StreamLayout.ABSENT_SSRC, false);
                var descriptors = publisher.buildStreamDescriptors(layout).streamDescriptors();
                // audio media+FEC+NACK (3) + app-data single (1) = 4 descriptors, no video.
                assertEquals(4, descriptors.size());
                assertTrue(descriptors.stream().allMatch(d -> d.ssrc().orElse(0) == 0x1111 || d.ssrc().orElse(0) == 0x2222));
                var payloadTypes = descriptors.stream()
                        .map(d -> d.payloadType().orElseThrow())
                        .toList();
                assertTrue(payloadTypes.contains(StreamDescriptor.PayloadType.MEDIA));
                assertTrue(payloadTypes.contains(StreamDescriptor.PayloadType.FEC));
                assertTrue(payloadTypes.contains(StreamDescriptor.PayloadType.NACK));
                assertTrue(payloadTypes.contains(StreamDescriptor.PayloadType.APP_DATA));
            } finally {
                publisher.close();
            }
        }

        @Test
        @DisplayName("an absent SSRC contributes no descriptors so an audio-only layout yields exactly three")
        void absentSsrcOmitted() {
            var publisher = newPublisher();
            try {
                var descriptors = publisher.buildStreamDescriptors(StreamLayout.audioOnly(0x55))
                        .streamDescriptors();
                assertEquals(3, descriptors.size());
                assertTrue(descriptors.stream().allMatch(d -> d.ssrc().orElse(0) == 0x55));
            } finally {
                publisher.close();
            }
        }

        @Test
        @DisplayName("publishRxSubscription suppresses an identical resend and frames a changed one")
        void publishSuppression() {
            var publisher = newPublisher();
            try {
                var rx = new RxSubscriptionsBuilder().vidRxPids(List.of(1)).build();
                var first = publisher.publishRxSubscription(rx, 0L);
                assertTrue(first.isPresent(), "the first publish is always framed");
                assertEquals(0x4021, first.orElseThrow().attributeType());
                assertTrue(publisher.publishRxSubscription(rx, 1L).isEmpty(),
                        "an identical subscription is suppressed");
                var changed = new RxSubscriptionsBuilder().vidRxPids(List.of(1, 2)).build();
                assertTrue(publisher.publishRxSubscription(changed, 2L).isPresent(),
                        "a changed subscription is framed again");
            } finally {
                publisher.close();
            }
        }

        private RxVidSubscriptionInfo vidInfo(int pid, RxVidSubscriptionInfo.VideoQuality quality) {
            return new RxVidSubscriptionInfoBuilder().pid(pid).vidQuality(quality).build();
        }

        private LiveSubscriptionPublisher newPublisher() {
            return new LiveSubscriptionPublisher(new TimerHeap(), () -> 0L, _ -> {
            });
        }
    }

    @Nested
    @DisplayName("RTCP-rx feedback subscription table (SPEC 14.3)")
    class RtcpRxTable {
        @Test
        @DisplayName("subscribe stores the entry, re-subscribe updates in place, unsubscribe frees the slot")
        void subscribeLifecycle() {
            var table = new RtcpRxSubscriptionTable();
            assertTrue(table.subscribe(0xAAAA, 0xBBBB, RtcpRxSubscriptionEntry.FLAG_NACK));
            assertEquals(1, table.size());
            var entry = table.entries().getFirst();
            assertEquals(0xBBBB, entry.mediaSsrc());
            assertTrue(entry.wantsNack());

            // Re-subscribe the same media SSRC overwrites the flags in place, no new slot.
            assertTrue(table.subscribe(0xAAAA, 0xBBBB,
                    RtcpRxSubscriptionEntry.FLAG_PLI | RtcpRxSubscriptionEntry.FLAG_FIR));
            assertEquals(1, table.size());
            var updated = table.entries().getFirst();
            assertTrue(updated.wantsPli());
            assertTrue(updated.wantsFir());
            assertFalse(updated.wantsNack());

            assertTrue(table.unsubscribe(0xBBBB));
            assertTrue(table.isEmpty());
        }

        @Test
        @DisplayName("the table saturates at the native 96-slot capacity for new media SSRCs")
        void saturatesAt96() {
            var table = new RtcpRxSubscriptionTable();
            for (var i = 0; i < RtcpRxSubscriptionTable.MAX_ENTRIES; i++) {
                assertTrue(table.subscribe(i, 1000 + i, RtcpRxSubscriptionEntry.FLAG_NACK));
            }
            assertEquals(96, table.size());
            assertFalse(table.subscribe(999, 9999, RtcpRxSubscriptionEntry.FLAG_NACK),
                    "a new media SSRC past 96 slots is rejected");
            // An update of an existing media SSRC still succeeds when full.
            assertTrue(table.subscribe(0, 1000, RtcpRxSubscriptionEntry.FLAG_PLI));
        }
    }

    @Nested
    @DisplayName("inbound demux: relay-leg classification of STUN, DTLS, and SRTP media")
    class RelayClassification {
        @Test
        @DisplayName("a STUN binding request classifies as STUN, never DTLS")
        void relayStunIsStun() {
            // The ICE connectivity check toward the relay (leading byte 0x00) is STUN, distinct from the
            // DTLS ContentType range; the socket demux routes it to the ICE agent.
            var binding = new StunMessage(StunMessage.TYPE_BINDING_REQUEST, StunMessage.MAGIC_COOKIE,
                    HEX.parseHex("0102030405060708090a0b0c"), List.of()).encode();
            assertEquals(PacketClass.STUN, InboundPacketDemux.classify(binding));
            assertNotEquals(PacketClass.DTLS, InboundPacketDemux.classify(binding));
        }

        @Test
        @DisplayName("a relay-leg DTLS record routes to the DTLS handler (the data channel's DTLS transport)")
        void relayDtlsRoutesToBridge() {
            // The relay path runs DTLS over the ICE-selected path (web-transport-construction-RE.md), so a
            // DTLS-range leading byte is routed to the DTLS handler, not dropped.
            var stunHits = new int[1];
            var dtls = new ArrayList<byte[]>();
            var mediaHits = new int[1];
            var demux = new InboundPacketDemux(
                    (bytes, source) -> stunHits[0]++, dtls::add, _ -> mediaHits[0]++, null);
            // 0x16 is the DTLS handshake ContentType.
            var record = new byte[]{0x16, (byte) 0xfe, (byte) 0xfd};
            assertEquals(PacketClass.DTLS, demux.accept(record, null));
            assertEquals(1, dtls.size());
            assertSame(record, dtls.getFirst());
            assertEquals(0, stunHits[0]);
            assertEquals(0, mediaHits[0]);
        }

        @Test
        @DisplayName("SRTP media from the relay classifies as RTP and routes to the media unprotect path")
        void relayMediaIsRtp() {
            var media = new ArrayList<byte[]>();
            var demux = new InboundPacketDemux(null, null, media::add, null);
            var rtp = new byte[]{(byte) 0x80, 0x6f, 0, 1};
            assertEquals(PacketClass.RTP, demux.accept(rtp, null));
            assertEquals(1, media.size());
            assertSame(rtp, media.getFirst());
        }
    }

    @Nested
    @DisplayName("HBH-SRTP protect/unprotect via libsrtp (SPEC 14.4) - skipped when native lib absent")
    class HbhSrtpRoundTrip {
        // A non-directional hop-by-hop media SRTP master: 16-byte AES-128 key + 14-byte salt = 30 bytes.
        private final byte[] master = HEX.parseHex(
                "000102030405060708090a0b0c0d0e0f"   // 16-byte key
                        + "101112131415161718191a1b1c1d"); // 14-byte salt
        // A distinct non-directional hop-by-hop SRTCP master keying the RTCP transforms: 16-byte key + 14-byte salt.
        private final byte[] srtcpMaster = HEX.parseHex(
                "202122232425262728292a2b2c2d2e2f"   // 16-byte key
                        + "303132333435363738393a3b3c3d"); // 14-byte salt

        @Test
        @DisplayName("an RTP packet protected then unprotected on the symmetric relay master round-trips")
        void rtpRoundTrip() {
            var relayPair = openRelays();
            var sender = relayPair[0];
            var receiver = relayPair[1];
            try {
                // A minimal RTP packet: version 2, PT 96, seq 1, ts 0, ssrc 0x11223344, 4 payload bytes,
                // with trailing room for the 80-bit auth tag.
                var rtp = HEX.parseHex("8060000100000000112233440a0b0c0d");
                var buffer = Arrays.copyOf(rtp, rtp.length + 64);
                var protectedLength = sender.protectRtp(buffer, rtp.length);
                assertTrue(protectedLength > rtp.length, "protect appends an auth tag");
                assertFalse(Arrays.equals(rtp, Arrays.copyOf(buffer, rtp.length)),
                        "the payload is encrypted in place");

                var clearLength = receiver.unprotectRtp(buffer, protectedLength);
                assertEquals(rtp.length, clearLength, "unprotect strips the auth tag back to the original length");
                assertArrayEquals(rtp, Arrays.copyOf(buffer, clearLength), "the cleartext round-trips");
            } finally {
                sender.close();
                receiver.close();
            }
        }

        @Test
        @DisplayName("an unprotect of a tampered packet fails authentication rather than returning cleartext")
        void tamperedAuthFails() {
            var relayPair = openRelays();
            var sender = relayPair[0];
            var receiver = relayPair[1];
            try {
                var rtp = HEX.parseHex("8060000200000000112233440a0b0c0d");
                var buffer = Arrays.copyOf(rtp, rtp.length + 64);
                var protectedLength = sender.protectRtp(buffer, rtp.length);
                // Flip a payload byte; libsrtp must reject the auth tag mismatch.
                buffer[12] ^= 0x01;
                final var len = protectedLength;
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                        () -> receiver.unprotectRtp(buffer, len));
            } finally {
                sender.close();
                receiver.close();
            }
        }

        // Two relay contexts keyed by the SAME master, modelling this client's outbound session and the
        // remote view of it (the relay forwards on one hop-by-hop context per SPEC 14.4). Skips the test
        // when the combined cobalt-native library or libsrtp is not present on the build host.
        private LiveHbhSrtpRelay[] openRelays() {
            try {
                var a = new LiveHbhSrtpRelay(master.clone(), SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80);
                var b = new LiveHbhSrtpRelay(master.clone(), SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80);
                return new LiveHbhSrtpRelay[]{a, b};
            } catch (Throwable t) {
                Assumptions.abort("native libsrtp unavailable on this host: " + t);
                throw new AssertionError("unreachable");
            }
        }
    }
}
