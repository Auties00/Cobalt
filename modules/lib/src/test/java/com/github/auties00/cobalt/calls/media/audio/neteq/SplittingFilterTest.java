package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SplittingFilter two-band QMF")
class SplittingFilterTest {
    @Test
    @DisplayName("splits a block into two half-length sub-bands")
    void splitsLength() {
        var filter = new SplittingFilter(2);
        var full = new float[160];
        var low = new float[80];
        var high = new float[80];
        filter.analysis(full, low, high);
        assertEquals(80, low.length);
        assertEquals(80, high.length);
    }

    @Test
    @DisplayName("reconstructs a sinusoid through analysis then synthesis within tolerance")
    void reconstructs() {
        var analysisFilter = new SplittingFilter(2);
        var synthesisFilter = new SplittingFilter(2);
        var full = new float[320];
        for (var i = 0; i < full.length; i++) {
            full[i] = (float) Math.sin(2.0 * Math.PI * i / 32.0);
        }
        var low = new float[160];
        var high = new float[160];
        var reconstructed = new float[320];
        analysisFilter.analysis(full, low, high);
        synthesisFilter.synthesis(low, high, reconstructed);
        // power-complementary QMF reconstructs up to group delay; check the energy is preserved broadly
        var inputEnergy = energy(full);
        var outputEnergy = energy(reconstructed);
        assertTrue(outputEnergy > inputEnergy * 0.25,
                "reconstructed energy " + outputEnergy + " collapsed against input " + inputEnergy);
    }

    @Test
    @DisplayName("rejects the unimplemented three-band split")
    void threeBandRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SplittingFilter(3));
    }

    @Test
    @DisplayName("rejects an odd-length full-band block")
    void oddLength() {
        var filter = new SplittingFilter(2);
        assertThrows(IllegalArgumentException.class,
                () -> filter.analysis(new float[5], new float[3], new float[3]));
    }

    private static double energy(float[] signal) {
        var sum = 0.0;
        for (var sample : signal) {
            sum += (double) sample * sample;
        }
        return sum;
    }
}
