package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.MLowAudioDecoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays the lifted live-capture MLow decode corpus through the wired {@link MLowAudioDecoder} and
 * checks that the reconstructed PCM tracks the reference the live decoder produced.
 *
 * <p>Each capture file is one continuous decoder stream, so a fresh decoder replays every packet of a
 * file in order to thread the cross-packet CELP state, and the two files are decoded independently. The
 * wired decoder runs the full shipping decode (the CELP kernel plus the harmonic, high-pass, and tilt
 * postfilters), so its output reproduces the live {@code DecodeInternal} reference at the packet start with
 * no algorithmic-delay offset ({@value #ALIGN_OFFSET}) over a {@value #WINDOW}-sample window. Because the
 * postfilter supplies the live decode's level and harmonic shaping, the reconstruction tracks the reference
 * at a least-squares gain of about one (the flat output gain the postfilter-off path needed is gone), so
 * the median Pearson correlation is at the ceiling; the least-squares gain fit and the signal-to-noise
 * ratio are measured only for reporting. A few frames at stream start (cold LPC memory) and around the one
 * live-stream discontinuity (concealment, out of scope) diverge, so the bar is the median and the fraction
 * of frames clearing the threshold, not every frame.
 */
class MlowDecoderParityTest {
    /**
     * The alignment offset in samples between the wired decoder output and the captured window.
     *
     * <p>Zero: the wired decoder runs the postfilter chain the live decoder runs, so its output aligns with
     * the live {@code DecodeInternal} reference at the packet start. The postfilter-off kernel alone needed
     * a codec-algorithmic-delay offset to line up against the postfilter-on reference; the wired postfilter
     * path does not.
     */
    private static final int ALIGN_OFFSET = 0;

    /**
     * The per-frame comparison window in samples.
     */
    private static final int WINDOW = 240;

    /**
     * The minimum acceptable median Pearson correlation across a file's frames.
     */
    private static final double MIN_MEDIAN_PEARSON = 0.99;

    /**
     * The minimum acceptable fraction of frames whose Pearson correlation clears {@value #FRAME_PEARSON}.
     */
    private static final double MIN_PASS_RATE = 0.90;

    /**
     * The per-frame Pearson threshold the pass-rate counts against.
     */
    private static final double FRAME_PEARSON = 0.99;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"parity-3.json", "parity-4.json"})
    @DisplayName("decodes the live capture to PCM matching the reference at >=0.99 median Pearson")
    void parity(String capture) {
        var corpus = MlowDecodeCorpus.loadResource(capture);
        var pairs = corpus.pairs();
        assertTrue(pairs.size() > 100, "expected a non-trivial corpus for " + capture);

        var decoder = new MLowAudioDecoder(16_000, 1);
        var decoded = new ArrayList<short[]>(pairs.size());
        for (var pair : pairs) {
            decoded.add(decoder.decode(pair.encoded(), 960, false));
        }

        var gain = leastSquaresGain(pairs, decoded);
        var correlations = new ArrayList<Double>();
        var snrs = new ArrayList<Double>();
        for (var i = 0; i < pairs.size(); i++) {
            var reference = pairs.get(i).expectedPcm();
            var output = decoded.get(i);
            if (ALIGN_OFFSET + WINDOW > output.length) {
                continue;
            }
            correlations.add(pearson(reference, output));
            snrs.add(signalToNoiseRatio(reference, output, gain));
        }
        Collections.sort(correlations);
        Collections.sort(snrs);

        var medianPearson = correlations.get(correlations.size() / 2);
        var passing = correlations.stream().filter(value -> value >= FRAME_PEARSON).count();
        var passRate = (double) passing / correlations.size();

        // surfaced in the Surefire report so the parity numbers are visible without re-running the probe
        System.out.printf(
                "%s: frames=%d gain=%.4f medianPearson=%.6f passRate(>=%.2f)=%.1f%% medianSNR=%.2f dB%n",
                capture, correlations.size(), gain, medianPearson, FRAME_PEARSON, 100.0 * passRate,
                snrs.get(snrs.size() / 2));

        assertTrue(medianPearson >= MIN_MEDIAN_PEARSON,
                "median Pearson " + medianPearson + " below " + MIN_MEDIAN_PEARSON + " for " + capture);
        assertTrue(passRate >= MIN_PASS_RATE,
                "Pearson pass-rate " + passRate + " below " + MIN_PASS_RATE + " for " + capture);
    }

    private static double leastSquaresGain(List<MlowDecodeCorpus.Pair> pairs, List<short[]> decoded) {
        var numerator = 0.0;
        var denominator = 0.0;
        for (var i = 0; i < pairs.size(); i++) {
            var reference = pairs.get(i).expectedPcm();
            var output = decoded.get(i);
            if (ALIGN_OFFSET + WINDOW > output.length) {
                continue;
            }
            for (var k = 0; k < WINDOW; k++) {
                numerator += (double) reference[k] * output[ALIGN_OFFSET + k];
                denominator += (double) output[ALIGN_OFFSET + k] * output[ALIGN_OFFSET + k];
            }
        }
        return denominator <= 0 ? 1.0 : numerator / denominator;
    }

    private static double pearson(short[] reference, short[] output) {
        var meanReference = 0.0;
        var meanOutput = 0.0;
        for (var i = 0; i < WINDOW; i++) {
            meanReference += reference[i];
            meanOutput += output[ALIGN_OFFSET + i];
        }
        meanReference /= WINDOW;
        meanOutput /= WINDOW;
        var varianceReference = 0.0;
        var varianceOutput = 0.0;
        var covariance = 0.0;
        for (var i = 0; i < WINDOW; i++) {
            var deltaReference = reference[i] - meanReference;
            var deltaOutput = output[ALIGN_OFFSET + i] - meanOutput;
            varianceReference += deltaReference * deltaReference;
            varianceOutput += deltaOutput * deltaOutput;
            covariance += deltaReference * deltaOutput;
        }
        if (varianceReference <= 0 || varianceOutput <= 0) {
            return 0.0;
        }
        return covariance / Math.sqrt(varianceReference * varianceOutput);
    }

    private static double signalToNoiseRatio(short[] reference, short[] output, double gain) {
        var signal = 0.0;
        var error = 0.0;
        for (var i = 0; i < WINDOW; i++) {
            double referenceSample = reference[i];
            var outputSample = gain * output[ALIGN_OFFSET + i];
            signal += referenceSample * referenceSample;
            var difference = referenceSample - outputSample;
            error += difference * difference;
        }
        if (error <= 0) {
            return 999.0;
        }
        if (signal <= 0) {
            return -999.0;
        }
        return 10.0 * Math.log10(signal / error);
    }
}
