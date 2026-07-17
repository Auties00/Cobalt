package com.github.auties00.cobalt.calls.media.audio.neteq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PushSincResampler windowed-sinc resampler")
class PushSincResamplerTest {
    @Test
    @DisplayName("computes the destination length from the rate ratio")
    void destinationLength() {
        var resampler = new PushSincResampler(48000, 16000, 480);
        assertEquals(160, resampler.destinationBlockLength());
    }

    @Test
    @DisplayName("passes a constant signal through at unit gain")
    void constantPassthrough() {
        var resampler = new PushSincResampler(16000, 8000, 320);
        var source = new float[320];
        java.util.Arrays.fill(source, 0.5f);
        var destination = new float[resampler.destinationBlockLength()];
        resampler.resample(source, destination);
        // a flat input must come out flat at the same level, away from the edge transient
        for (var i = 8; i < destination.length - 8; i++) {
            assertEquals(0.5f, destination[i], 1e-3f, "sample " + i + " drifted");
        }
    }

    @Test
    @DisplayName("downsamples a low-frequency sinusoid preserving its shape")
    void downsampleSinusoid() {
        var resampler = new PushSincResampler(32000, 16000, 640);
        var source = new float[640];
        for (var i = 0; i < source.length; i++) {
            source[i] = (float) Math.sin(2.0 * Math.PI * 500.0 * i / 32000.0);
        }
        var destination = new float[resampler.destinationBlockLength()];
        resampler.resample(source, destination);
        // the resampled tone must still be bounded and non-trivial
        var peak = 0.0f;
        for (var sample : destination) {
            peak = Math.max(peak, Math.abs(sample));
        }
        assertTrue(peak > 0.5f && peak < 1.5f, "resampled peak out of range: " + peak);
    }

    @Test
    @DisplayName("rejects a source block of the wrong length")
    void wrongSourceLength() {
        var resampler = new PushSincResampler(16000, 16000, 320);
        assertThrows(IllegalArgumentException.class,
                () -> resampler.resample(new float[160], new float[320]));
    }
}
