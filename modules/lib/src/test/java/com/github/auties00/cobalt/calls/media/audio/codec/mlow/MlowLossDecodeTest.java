package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.MLowAudioDecoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays the deterministic oracle packet-loss scenarios through the wired {@link MLowAudioDecoder} and
 * checks the concealed and forward-error-correction-recovered PCM against the C decoder reference.
 *
 * <p>Each fixture under {@code fixtures/call/mlow/loss/} is one continuous 16 kHz mono 60 ms MLow stream decoded by
 * the shipping C decoder (LPC postfilter on) under a fixed lost-frame set. The {@code fec} stream carries
 * in-band redundancy so every lost frame is recovered from the following packet's leading LBRR copy; the
 * {@code plc} stream carries none so every lost frame is concealed. The Java decoder must replay the
 * identical loss sequence, because a loss perturbs the decoder state a received frame after it inherits.
 * Forward-error-correction recovery decodes real transmitted data and so is held to a near-exact bar; packet
 * loss concealment is a plausible-continuation synthesis held to a signal-to-noise-ratio bar, with the loud
 * voiced concealment frames (where the concealment is audible) held tighter than the aggregate.
 */
class MlowLossDecodeTest {
    private static final int FRAME = 960;

    private static final String RESOURCE_DIR = "fixtures/call/mlow/loss/";

    @Test
    @DisplayName("in-band FEC recovers each lost frame from the next packet's LBRR copy, near-exact vs the reference")
    void fecRecovery() {
        var result = replay("fec");
        assertTrue(result.overallSnr >= 60.0,
                "FEC-recovered stream SNR " + result.overallSnr + " dB below 60 dB");
    }

    @Test
    @DisplayName("packet-loss concealment tracks the reference, with loud voiced concealment frames near float tolerance")
    void concealment() {
        var result = replay("plc");
        assertTrue(result.overallSnr >= 30.0,
                "concealed stream SNR " + result.overallSnr + " dB below 30 dB");
        // The loud voiced concealment frames (where the concealment is audible) reproduce the reference near
        // float tolerance; the median guards the voiced noise-injection path without being brittle on a
        // single transitional frame at a voice-activity boundary.
        var loudSnrs = result.concealedFrames.stream()
                .filter(frame -> frame.referenceRms > 4000.0)
                .map(ConcealedFrame::snr)
                .sorted()
                .toList();
        assertTrue(loudSnrs.size() >= 3, "expected several loud voiced conceal frames, saw " + loudSnrs.size());
        var medianLoud = loudSnrs.get(loudSnrs.size() / 2);
        assertTrue(medianLoud >= 40.0,
                "median loud voiced conceal SNR " + medianLoud + " dB below 40 dB (voiced noise-injection regression)");
    }

    private record ConcealedFrame(int index, double snr, double referenceRms) {
    }

    private record ReplayResult(double overallSnr, List<ConcealedFrame> concealedFrames) {
    }

    private static ReplayResult replay(String prefix) {
        var packets = readPackets(prefix);
        var reference = readPcm(prefix + "_decoded.pcm");
        var modes = readModes(prefix + "_frame_modes.txt");

        var decoder = new MLowAudioDecoder(16_000, 1, true);
        var out = new short[reference.length];
        for (var mode : modes) {
            var j = mode[0];
            short[] frame = switch (mode[1]) {
                case 0 -> decoder.decode(packets.get(j), FRAME, false);
                case 1 -> decoder.decode(packets.get(mode[2]), FRAME, true);
                case 2 -> decoder.conceal(FRAME);
                default -> throw new IllegalStateException("mode " + mode[1]);
            };
            System.arraycopy(frame, 0, out, j * FRAME, Math.min(FRAME, frame.length));
        }

        var concealed = new ArrayList<ConcealedFrame>();
        for (var mode : modes) {
            if (mode[1] != 0) {
                var j = mode[0];
                concealed.add(new ConcealedFrame(j, snr(reference, out, j * FRAME, FRAME),
                        rms(reference, j * FRAME, FRAME)));
            }
        }
        return new ReplayResult(snr(reference, out, 0, reference.length), concealed);
    }

    private static double snr(short[] ref, short[] out, int off, int len) {
        double signal = 0;
        double error = 0;
        for (var i = 0; i < len; i++) {
            double r = ref[off + i];
            double e = r - out[off + i];
            signal += r * r;
            error += e * e;
        }
        if (error <= 0) {
            return 999.0;
        }
        if (signal <= 0) {
            return -999.0;
        }
        return 10.0 * Math.log10(signal / error);
    }

    private static double rms(short[] pcm, int off, int len) {
        double sum = 0;
        for (var i = 0; i < len; i++) {
            sum += (double) pcm[off + i] * pcm[off + i];
        }
        return Math.sqrt(sum / len);
    }

    private static List<byte[]> readPackets(String prefix) {
        var bin = readBytes(prefix + "_packets.bin");
        var packets = new ArrayList<byte[]>();
        var offset = 0;
        for (var line : readText(prefix + "_packet_sizes.txt").split("\\R")) {
            var t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            var size = Integer.parseInt(t);
            packets.add(java.util.Arrays.copyOfRange(bin, offset, offset + size));
            offset += size;
        }
        return packets;
    }

    private static short[] readPcm(String name) {
        var bytes = readBytes(name);
        var pcm = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm);
        return pcm;
    }

    private static List<int[]> readModes(String name) {
        var modes = new ArrayList<int[]>();
        for (var line : readText(name).split("\\R")) {
            var t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            var parts = t.split("\\s+");
            modes.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])});
        }
        return modes;
    }

    private static byte[] readBytes(String name) {
        try (InputStream in = MlowLossDecodeTest.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + name)) {
            if (in == null) {
                throw new UncheckedIOException(new IOException("resource not found: " + RESOURCE_DIR + name));
            }
            return in.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String readText(String name) {
        return new String(readBytes(name), StandardCharsets.UTF_8);
    }
}
