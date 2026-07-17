package com.github.auties00.cobalt.calls.media.audio.pipeline;

import com.github.auties00.cobalt.calls.media.audio.neteq.LiveNetEq;
import com.github.auties00.cobalt.calls.media.audio.neteq.NetEqConfig;
import com.github.auties00.cobalt.calls.stream.AudioFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The receiver is a thin adapter over LiveNetEq.getAudio(): a stub NetEqAudioSource proves it copies the
// served frame, records the source's voice verdict, honours the pull-length truncation, and delegates insert.
// A real LiveNetEq driven through the receiver proves the 60 ms MLow (960-sample) decode is served across
// three pulls from the SyncBuffer (the mechanism that replaced the decoded-remainder FIFO), and that the
// single 20 ms Opus decode is one frame per pull.
@DisplayName("AudioDecoderReceiver getAudio adapter")
class AudioDecoderReceiverTest {
    private static final int FRAME = AudioDecoderReceiver.FRAME_SAMPLES;

    // A NetEqAudioSource returning a queued frame per getAudio() call with a queued voice verdict, recording
    // inserted packets so the receiver's insert delegation is observable.
    private static final class StubSource implements AudioDecoderReceiver.NetEqAudioSource {
        private final Deque<short[]> frames = new ArrayDeque<>();
        private final Deque<Boolean> verdicts = new ArrayDeque<>();
        private boolean lastVoiceActive;
        private int inserts;

        @Override
        public void insert(int rtpSequence, long rtpTimestamp, byte[] payload) {
            inserts++;
        }

        @Override
        public AudioFrame getAudio() {
            var pcm = frames.poll();
            lastVoiceActive = Boolean.TRUE.equals(verdicts.poll());
            return new AudioFrame(pcm != null ? pcm : new short[FRAME], 0L);
        }

        @Override
        public boolean lastFrameVoiceActive() {
            return lastVoiceActive;
        }
    }

    private static short[] ramp(int length) {
        var out = new short[length];
        for (var i = 0; i < length; i++) {
            out[i] = (short) i;
        }
        return out;
    }

    @Nested
    @DisplayName("thin getAudio adapter")
    class Adapter {
        @Test
        @DisplayName("copies the served frame into the pull block at full geometry")
        void copiesServedFrame() {
            var source = new StubSource();
            source.frames.add(ramp(FRAME));
            source.verdicts.add(true);
            var receiver = new AudioDecoderReceiver(source);

            var block = new short[FRAME];
            assertEquals(FRAME, receiver.pull(block, FRAME));
            for (var i = 0; i < FRAME; i++) {
                assertEquals(i, block[i], "the served frame must reach the pull block unchanged");
            }
        }

        @Test
        @DisplayName("records the source's voice-activity verdict per pull")
        void recordsVoiceVerdict() {
            var source = new StubSource();
            source.frames.add(ramp(FRAME));
            source.verdicts.add(true);
            source.frames.add(ramp(FRAME));
            source.verdicts.add(false);
            var receiver = new AudioDecoderReceiver(source);

            receiver.pull(new short[FRAME], FRAME);
            assertTrue(receiver.lastFrameVoiceActive(), "an active served frame reports active");
            receiver.pull(new short[FRAME], FRAME);
            assertFalse(receiver.lastFrameVoiceActive(), "an inactive served frame reports inactive");
        }

        @Test
        @DisplayName("a pull for fewer than a whole frame truncates the served samples")
        void truncatesShortPull() {
            var source = new StubSource();
            source.frames.add(ramp(FRAME));
            source.verdicts.add(true);
            var receiver = new AudioDecoderReceiver(source);

            var block = new short[FRAME];
            var copied = receiver.pull(block, 100);
            assertEquals(100, copied, "a short pull returns the truncated count");
            for (var i = 0; i < 100; i++) {
                assertEquals(i, block[i]);
            }
        }

        @Test
        @DisplayName("a non-positive length copies nothing")
        void zeroLength() {
            var source = new StubSource();
            var receiver = new AudioDecoderReceiver(source);
            assertEquals(0, receiver.pull(new short[FRAME], 0));
        }

        @Test
        @DisplayName("receivePacket delegates the insert to the NetEq source")
        void delegatesInsert() {
            var source = new StubSource();
            var receiver = new AudioDecoderReceiver(source);
            receiver.receivePacket(7, 7L * FRAME, new byte[]{1});
            assertEquals(1, source.inserts, "the receiver must delegate the insert to the NetEq buffer");
        }
    }

    @Nested
    @DisplayName("driven over a real LiveNetEq")
    class OverLiveNetEq {
        // A FrameDecoder seam mapping each packet to a fixed sample array, with a per-decode voice verdict, so
        // a 960-sample multi-frame decode and a 320-sample single-frame decode are both expressible without
        // libopus. A ramp 0..n-1 makes each served frame's origin in the decoded packet visible.
        private record StubDecoder(short[] decoded, boolean voiceActive)
                implements AudioDecoderReceiver.FrameDecoder {
            @Override
            public AudioDecoderReceiver.DecodedFrame decode(byte[] payload, int frameSamples, boolean fec) {
                return new AudioDecoderReceiver.DecodedFrame(decoded, voiceActive);
            }

            @Override
            public short[] conceal(int frameSamples) {
                return new short[frameSamples];
            }
        }

        private AudioDecoderReceiver receiverFor(short[] decoded, boolean voiceActive) {
            var netEq = new LiveNetEq(NetEqConfig.defaults(), new StubDecoder(decoded, voiceActive));
            netEq.insert(1, 320L, new byte[]{1});
            return new AudioDecoderReceiver(netEq);
        }

        @Test
        @DisplayName("serves a 60 ms 960-sample decode as three ordered frames across three pulls")
        void mlowServedAcrossThreePulls() {
            var receiver = receiverFor(ramp(3 * FRAME), true);

            var first = new short[FRAME];
            var second = new short[FRAME];
            var third = new short[FRAME];
            assertEquals(FRAME, receiver.pull(first, FRAME));
            assertEquals(FRAME, receiver.pull(second, FRAME));
            assertEquals(FRAME, receiver.pull(third, FRAME));

            for (var i = 0; i < FRAME; i++) {
                assertEquals(i, first[i], "first frame is samples [0, FRAME)");
                assertEquals(FRAME + i, second[i], "second frame is samples [FRAME, 2*FRAME)");
                assertEquals(2 * FRAME + i, third[i], "third frame is samples [2*FRAME, 3*FRAME)");
            }
        }

        @Test
        @DisplayName("carries the packet's voice verdict across all three served frames")
        void mlowVoiceVerdictCarries() {
            var receiver = receiverFor(ramp(3 * FRAME), true);
            receiver.pull(new short[FRAME], FRAME);
            assertTrue(receiver.lastFrameVoiceActive(), "first frame reports the packet's verdict");
            receiver.pull(new short[FRAME], FRAME);
            assertTrue(receiver.lastFrameVoiceActive(), "second served frame keeps the verdict");
            receiver.pull(new short[FRAME], FRAME);
            assertTrue(receiver.lastFrameVoiceActive(), "third served frame keeps the verdict");
        }

        @Test
        @DisplayName("a 20 ms 320-sample decode renders one full frame per pull")
        void opusSingleFramePerPull() {
            var netEq = new LiveNetEq(NetEqConfig.defaults(), new StubDecoder(ramp(FRAME), true));
            for (var seq = 1; seq <= 3; seq++) {
                netEq.insert(seq, seq * 320L, new byte[]{1});
            }
            var receiver = new AudioDecoderReceiver(netEq);
            for (var pull = 0; pull < 3; pull++) {
                var block = new short[FRAME];
                assertEquals(FRAME, receiver.pull(block, FRAME));
                for (var i = 0; i < FRAME; i++) {
                    assertEquals(i, block[i], "every 20 ms decode is the full single frame");
                }
            }
        }

        @Test
        @DisplayName("an inactive packet renders the served frame inactive")
        void inactiveVerdict() {
            var receiver = receiverFor(ramp(FRAME), false);
            receiver.pull(new short[FRAME], FRAME);
            assertFalse(receiver.lastFrameVoiceActive());
        }
    }
}
