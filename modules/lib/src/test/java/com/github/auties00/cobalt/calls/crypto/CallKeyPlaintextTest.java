package com.github.auties00.cobalt.calls.crypto;

import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.call.CallOfferMessage;
import com.github.auties00.cobalt.wire.linked.message.call.CallOfferMessageBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.CallKeySignalSeamTest;

/**
 * Known-answer tests for the call-key plaintext wrap, the bytes
 * {@link LiveCallKeyExchange#wrapCallKey(byte[])} hands to the Signal cipher and
 * {@link LiveCallKeyExchange#decryptCallKey} reads back.
 *
 * <p>The wrap is the protobuf {@code LinkedMessageContainer{Call{callKey}}}: field 10 ({@code Message.call})
 * wrapping field 1 ({@code Call.callKey}, 32 bytes). The live oracle is the group-rekey sample decrypted
 * from a real session in {@code re/calls2-spec/captures/group-rekey.json}: the post-unpad plaintext
 * {@code 52220a20d919...223e} decodes to call key {@code d919...223e}. These tests pin the model wrap
 * (exactly what {@code wrapCallKey} encodes) rather than driving a live Signal session, which the final
 * gate has no key material for; the Signal seam itself is asserted structurally by
 * {@link com.github.auties00.cobalt.calls.CallKeySignalSeamTest}.
 */
public class CallKeyPlaintextTest {
    private static final HexFormat HEX = HexFormat.of();

    // group-rekey.json decryptedPlaintext.samples[0] (session "primary"): the live, real-session
    // post-unpad bytes and the 32-byte call key they carry.
    private static final byte[] LIVE_PLAINTEXT =
            HEX.parseHex("52220a20d919e20879fa56dbc3ae66a5fbc6c0ae0538be2f6fbe1253e593283d5103223e");
    private static final byte[] LIVE_CALL_KEY =
            HEX.parseHex("d919e20879fa56dbc3ae66a5fbc6c0ae0538be2f6fbe1253e593283d5103223e");

    /**
     * Wraps a call key exactly as {@link LiveCallKeyExchange#wrapCallKey(byte[])} does, without the
     * Signal pipeline collaborators.
     */
    private static byte[] wrap(byte[] callKey) {
        var offer = new CallOfferMessageBuilder().callKey(callKey).build();
        var container = new LinkedMessageContainerBuilder().call(offer).build();
        return LinkedMessageContainerSpec.encode(container);
    }

    @Nested
    @DisplayName("live sample (group-rekey.json, decrypted from a real session)")
    class LiveSample {
        @Test
        @DisplayName("decodes the live plaintext to the captured 32-byte call key")
        public void decodesLiveSample() {
            var container = LinkedMessageContainerSpec.decode(LIVE_PLAINTEXT);
            var offer = assertInstanceOf(CallOfferMessage.class, container.content());
            assertTrue(offer.callKey().isPresent(), "decoded container must carry a call key");
            assertArrayEquals(LIVE_CALL_KEY, offer.callKey().orElseThrow());
        }

        @Test
        @DisplayName("re-encoding the captured key reproduces the live plaintext bytes")
        public void reencodesToLiveBytes() {
            // The live plaintext is the canonical Message{Call{callKey}} with no other fields set, so the
            // wrap must reproduce it byte-for-byte (the 0a20 tag is the only sub-field; the key's own tail
            // happens to be ...223e, which the capture note mislabels as a trailing field).
            assertArrayEquals(LIVE_PLAINTEXT, wrap(LIVE_CALL_KEY));
        }
    }

    @Nested
    @DisplayName("round-trip")
    class RoundTrip {
        @Test
        @DisplayName("encoding then decoding a 32-byte key yields the same key")
        public void roundTrips() {
            var key = new byte[32];
            for (var i = 0; i < key.length; i++) {
                key[i] = (byte) (0xA0 + i);
            }
            var encoded = wrap(key);
            var container = LinkedMessageContainerSpec.decode(encoded);
            var offer = assertInstanceOf(CallOfferMessage.class, container.content());
            assertArrayEquals(key, offer.callKey().orElseThrow());
        }

        @Test
        @DisplayName("the wrap is the canonical 36-byte 52 22 0a 20 framing")
        public void framing() {
            var key = new byte[32];
            var encoded = wrap(key);
            assertEquals(36, encoded.length, "2-byte outer tag + 2-byte inner tag + 32-byte key");
            assertEquals((byte) 0x52, encoded[0], "field 10 (Message.call), wire type 2");
            assertEquals((byte) 0x22, encoded[1], "outer length 0x22 = 34");
            assertEquals((byte) 0x0a, encoded[2], "field 1 (Call.callKey), wire type 2");
            assertEquals((byte) 0x20, encoded[3], "inner length 0x20 = 32");
        }
    }

    @Nested
    @DisplayName("call-key length contract")
    class Length {
        @Test
        @DisplayName("CALL_KEY_LENGTH is 32")
        public void length() {
            assertEquals(32, LiveCallKeyExchange.CALL_KEY_LENGTH);
        }
    }
}
