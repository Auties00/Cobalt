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
 * Guards the normal decode of an in-band forward-error-correction packet (TOC bit1, {@code 0x52}) against
 * the leading-LBRR mis-decode: such a packet carries a redundant lower-bitrate copy of the previous frame
 * ahead of the primary payload, and the decoder must skip past it before reading the main frame. The
 * corpus holds ~75 of these ({@code 0x52}) alongside the ~3282 clean ({@code 0x50}) packets; before the
 * {@code skip_bits} fix they decoded to noise (negative correlation), which the aggregate
 * {@link MlowDecoderParityTest} median masked because they are only about two percent of the stream. This
 * suite isolates the FEC-flagged packets and asserts they track the reference as closely as the clean ones.
 */
class MlowFecDecodeTest {
    private static final int WINDOW = 240;

    private static final double MIN_MEDIAN_PEARSON = 0.99;

    private static final double MIN_FRAME_PEARSON = 0.9;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"parity-3.json", "parity-4.json"})
    @DisplayName("FEC-flagged (0x52) packets decode to the reference, not the leading LBRR copy")
    void fecPacketsDecodeToMainFrame(String capture) {
        var pairs = MlowDecodeCorpus.loadResource(capture).pairs();
        var decoder = new MLowAudioDecoder(16_000, 1);
        var fecCorrelations = new ArrayList<Double>();
        for (var pair : pairs) {
            var decoded = decoder.decode(pair.encoded(), 960, false);
            if (WINDOW > decoded.length) {
                continue;
            }
            if (MlowTocByte.decode(pair.encoded()[0] & 0xFF).fec()) {
                fecCorrelations.add(pearson(pair.expectedPcm(), decoded));
            }
        }

        assertTrue(fecCorrelations.size() >= 10,
                "expected a non-trivial FEC-flagged population in " + capture + ", saw " + fecCorrelations.size());
        Collections.sort(fecCorrelations);
        var median = fecCorrelations.get(fecCorrelations.size() / 2);
        var min = fecCorrelations.get(0);
        assertTrue(median >= MIN_MEDIAN_PEARSON,
                "FEC median Pearson " + median + " below " + MIN_MEDIAN_PEARSON + " for " + capture);
        assertTrue(min >= MIN_FRAME_PEARSON,
                "worst FEC frame Pearson " + min + " below " + MIN_FRAME_PEARSON + " for " + capture
                        + " (leading-LBRR mis-decode regression)");
    }

    private static double pearson(short[] reference, short[] output) {
        var meanReference = 0.0;
        var meanOutput = 0.0;
        for (var i = 0; i < WINDOW; i++) {
            meanReference += reference[i];
            meanOutput += output[i];
        }
        meanReference /= WINDOW;
        meanOutput /= WINDOW;
        var varianceReference = 0.0;
        var varianceOutput = 0.0;
        var covariance = 0.0;
        for (var i = 0; i < WINDOW; i++) {
            var deltaReference = reference[i] - meanReference;
            var deltaOutput = output[i] - meanOutput;
            varianceReference += deltaReference * deltaReference;
            varianceOutput += deltaOutput * deltaOutput;
            covariance += deltaReference * deltaOutput;
        }
        if (varianceReference <= 0 || varianceOutput <= 0) {
            return 0.0;
        }
        return covariance / Math.sqrt(varianceReference * varianceOutput);
    }
}
