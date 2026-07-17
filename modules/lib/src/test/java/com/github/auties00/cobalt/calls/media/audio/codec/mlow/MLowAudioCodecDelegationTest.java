package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.MLowAudioDecoder;

/**
 * Confirms that {@link MLowAudioCodec}'s decode, in-band forward-error-correction, and loss-recovery paths
 * delegate to the wrapped {@link com.github.auties00.cobalt.calls.media.audio.neteq.decoder.MLowAudioDecoder} rather than throwing.
 * The decode fidelity is covered by the decoder's own parity, forward-error-correction, and concealment
 * suites; this suite only exercises the codec's delegation surface (a real corpus packet, plus the recovery
 * and concealment entry points) and its lifecycle.
 */
class MLowAudioCodecDelegationTest {
    private static final int FRAME = 960;

    private static byte[] samplePacket() {
        return MlowDecodeCorpus.loadResource("parity-3.json").pairs().get(0).encoded();
    }

    @Nested
    @DisplayName("decode delegation")
    class Decode {
        @Test
        @DisplayName("a normal decode returns the packet's PCM")
        void normalDecode() {
            try (var codec = new MLowAudioCodec()) {
                var pcm = codec.decode(samplePacket(), FRAME, false);
                assertNotNull(pcm);
                assertEquals(FRAME, pcm.length, "a 60 ms MLow packet decodes to 960 samples");
            }
        }

        @Test
        @DisplayName("a forward-error-correction decode returns a full frame instead of throwing")
        void fecDecode() {
            try (var codec = new MLowAudioCodec()) {
                // Prime with a normal decode so the decoder has state, then request FEC on the next packet.
                var pairs = MlowDecodeCorpus.loadResource("parity-3.json").pairs();
                codec.decode(pairs.get(0).encoded(), FRAME, false);
                var recovered = codec.decode(pairs.get(1).encoded(), FRAME, true);
                assertNotNull(recovered);
                assertEquals(FRAME, recovered.length);
            }
        }
    }

    @Nested
    @DisplayName("recover delegation")
    class Recover {
        @Test
        @DisplayName("recover(null) conceals a lost frame instead of throwing")
        void concealment() {
            try (var codec = new MLowAudioCodec()) {
                codec.decode(samplePacket(), FRAME, false);
                var concealed = codec.recover(null, FRAME);
                assertNotNull(concealed);
                assertEquals(FRAME, concealed.length);
            }
        }

        @Test
        @DisplayName("recover(nextPayload) reconstructs from the following packet instead of throwing")
        void fecRecovery() {
            try (var codec = new MLowAudioCodec()) {
                var pairs = MlowDecodeCorpus.loadResource("parity-3.json").pairs();
                codec.decode(pairs.get(0).encoded(), FRAME, false);
                var recovered = codec.recover(pairs.get(1).encoded(), FRAME);
                assertNotNull(recovered);
                assertEquals(FRAME, recovered.length);
            }
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {
        @Test
        @DisplayName("close is idempotent and decoding after close throws")
        void closeThenDecodeThrows() {
            var codec = new MLowAudioCodec();
            codec.close();
            codec.close();
            assertThrows(IllegalStateException.class, () -> codec.decode(samplePacket(), FRAME, false));
        }
    }
}
