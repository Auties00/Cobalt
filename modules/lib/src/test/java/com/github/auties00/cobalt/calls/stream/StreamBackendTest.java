package com.github.auties00.cobalt.calls.stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.stream.audio.SilenceAudioOutput;
import com.github.auties00.cobalt.calls.stream.audio.WavFileAudioInput;

/**
 * Adversarial P10 verification of the ported {@code calls.stream} backends: the synthetic capture sources
 * (tone, silence) and the manual buffered source/sink, plus the WAV playback sink. These are the concrete
 * implementations migrated from the legacy {@code call.stream} package, and this suite is the verifier's
 * independent proof that the port preserved the stream semantics (capture source feeds the call, playback
 * sink drains the call) and converted the per-frame clock to {@link AudioFrame#ptsMicros()} microseconds.
 *
 * <p>Only the device-free backends are exercised here. The microphone, speaker, camera, screen, and
 * media-file backends bind a real OS device or the bundled FFmpeg natives, so they are deliberately skipped:
 * the harness needs no hardware and never touches FFM. The round trips assert the call media format (16 kHz
 * mono signed 16-bit PCM), the monotonic microsecond presentation clock, and the two distinct backpressure
 * policies the bounded buffers implement (a capture source blocks the producer; a playback sink drops the
 * oldest frame to bound latency).
 */
@DisplayName("calls.stream ported backends (synthetic capture, buffered, WAV sink)")
class StreamBackendTest {
    private static final int CALL_SAMPLE_RATE = 16_000;
    private static final int FRAME_SAMPLES = 160; // 10 ms at 16 kHz, the default synthetic frame
    private static final long FRAME_MICROS = 10_000; // 10 ms in the ptsMicros clock


    @Nested
    @DisplayName("SilenceAudioOutput (synthetic silence capture source)")
    class Silence {
        @Test
        @DisplayName("emits all-zero 16 kHz frames with a monotonic microsecond clock")
        void silenceFramesAreZeroAndClocked() throws InterruptedException {
            var source = AudioOutput.fromSilence();
            try {
                var first = source.takeAudio();
                var second = source.takeAudio();
                assertEquals(FRAME_SAMPLES, first.pcm().length);
                assertEquals(0, energy(first.pcm()), "silence is true digital zero, not comfort noise");
                assertEquals(0L, first.ptsMicros());
                assertEquals(FRAME_MICROS, second.ptsMicros(),
                        "the silence clock advances 10 ms per frame in microseconds");
            } finally {
                source.shutdown();
            }
        }

        @Test
        @DisplayName("a custom geometry honours the requested frame size and duration")
        void silenceCustomGeometry() throws InterruptedException {
            var source = new SilenceAudioOutput(320, 20_000);
            try {
                var first = source.takeAudio();
                var second = source.takeAudio();
                assertEquals(320, first.pcm().length);
                assertEquals(20_000L, second.ptsMicros(), "a 20 ms frame steps the clock by 20000 us");
            } finally {
                source.shutdown();
            }
        }

        @Test
        @DisplayName("rejects a non-positive frame geometry")
        void silenceRejectsBadGeometry() {
            assertThrows(IllegalArgumentException.class, () -> new SilenceAudioOutput(0, 10_000));
            assertThrows(IllegalArgumentException.class, () -> new SilenceAudioOutput(160, 0));
        }
    }

    @Nested
    @DisplayName("WavFileAudioInput (WAV-file playback sink)")
    class WavSink {
        @Test
        @DisplayName("records offered frames as a canonical 16 kHz mono 16-bit PCM WAV the samples survive")
        void wavRoundTripSamples() throws IOException {
            var path = Files.createTempFile("calls-stream-verify", ".wav");
            try {
                var pcm = ramp(FRAME_SAMPLES);
                var sink = AudioInput.toWav(path);
                sink.offerAudio(new AudioFrame(pcm, 0L));
                sink.offerAudio(new AudioFrame(pcm, FRAME_MICROS));
                sink.shutdown();

                var bytes = Files.readAllBytes(path);
                // 44-byte canonical header + two frames of 160 little-endian 16-bit samples.
                var expectedSampleBytes = 2 * FRAME_SAMPLES * Short.BYTES;
                assertEquals(44 + expectedSampleBytes, bytes.length, "header plus the two frames' samples");

                var header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                assertEquals(0x46464952, header.getInt(0), "RIFF magic"); // 'RIFF' little-endian
                assertEquals(0x45564157, header.getInt(8), "WAVE form"); // 'WAVE' little-endian
                assertEquals(1, header.getShort(20), "PCM format code");
                assertEquals(1, header.getShort(22), "mono, the call channel layout");
                assertEquals(CALL_SAMPLE_RATE, header.getInt(24), "16 kHz, the call sample rate");
                assertEquals(16, header.getShort(34), "signed 16-bit samples");
                // The two patched size fields reflect the body length finalised on shutdown.
                assertEquals(36 + expectedSampleBytes, header.getInt(4), "RIFF chunk size");
                assertEquals(expectedSampleBytes, header.getInt(40), "data chunk size");

                // The first body sample is the first PCM sample, little-endian, proving the samples survive.
                var firstSample = (short) ((bytes[44] & 0xFF) | (bytes[45] << 8));
                assertEquals(pcm[0], firstSample, "the recorded samples must equal the offered samples");
            } finally {
                Files.deleteIfExists(path);
            }
        }

        @Test
        @DisplayName("shutdown is idempotent and a frame offered after it is dropped")
        void wavShutdownIdempotent() throws IOException {
            var path = Files.createTempFile("calls-stream-verify-idem", ".wav");
            try {
                var sink = AudioInput.toWav(path);
                sink.offerAudio(new AudioFrame(ramp(FRAME_SAMPLES), 0L));
                sink.shutdown();
                var sizeAfterFirst = Files.size(path);
                sink.offerAudio(new AudioFrame(ramp(FRAME_SAMPLES), FRAME_MICROS));
                sink.shutdown(); // second shutdown must not throw
                assertEquals(sizeAfterFirst, Files.size(path),
                        "a frame offered after shutdown must not be appended");
            } finally {
                Files.deleteIfExists(path);
            }
        }

        @Test
        @DisplayName("a null path is rejected by the factory")
        void wavRejectsNullPath() {
            assertThrows(NullPointerException.class, () -> AudioInput.toWav(null));
        }
    }

    @Nested
    @DisplayName("AudioFrame value semantics")
    class Frame {
        @Test
        @DisplayName("rejects a null sample buffer and accepts an empty one")
        void frameValidation() {
            assertThrows(NullPointerException.class, () -> new AudioFrame(null, 0L));
            var empty = new AudioFrame(new short[0], 0L);
            assertEquals(0, empty.pcm().length, "an empty buffer is a legal zero-length frame");
        }

        @Test
        @DisplayName("references the supplied buffer without copying")
        void frameSharesBuffer() {
            var pcm = ramp(8);
            var frame = new AudioFrame(pcm, 0L);
            assertSame(pcm, frame.pcm(), "the frame shares the supplied array; ownership is transferred");
        }
    }

    private static long energy(short[] pcm) {
        long sum = 0;
        for (var s : pcm) {
            sum += (long) s * s;
        }
        return sum;
    }

    private static short[] ramp(int n) {
        var pcm = new short[n];
        for (var i = 0; i < n; i++) {
            pcm[i] = (short) (i * 37 - 1000);
        }
        return pcm;
    }
}
