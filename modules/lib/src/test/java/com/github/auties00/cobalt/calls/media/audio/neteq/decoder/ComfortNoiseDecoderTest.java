package com.github.auties00.cobalt.calls.media.audio.neteq.decoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ComfortNoiseDecoder RFC 3389 generation")
class ComfortNoiseDecoderTest {
    @Test
    @DisplayName("produces a frame of the requested sample count")
    void frameLength() {
        var decoder = new ComfortNoiseDecoder(16000, 1);
        var frame = decoder.decode(new byte[]{40}, 320, false);
        assertEquals(320, frame.length);
    }

    @Test
    @DisplayName("a loud descriptor produces louder noise than a quiet one")
    void levelDrivesAmplitude() {
        var loud = new ComfortNoiseDecoder(16000, 1);
        var quiet = new ComfortNoiseDecoder(16000, 1);
        var loudFrame = loud.decode(new byte[]{10}, 320, false);
        var quietFrame = quiet.decode(new byte[]{80}, 320, false);
        assertTrue(rms(loudFrame) > rms(quietFrame),
                "loud rms " + rms(loudFrame) + " not above quiet rms " + rms(quietFrame));
    }

    @Test
    @DisplayName("conceals with the last descriptor without a new payload")
    void concealReusesDescriptor() {
        var decoder = new ComfortNoiseDecoder(16000, 1);
        decoder.decode(new byte[]{30}, 320, false);
        var concealed = decoder.conceal(320);
        assertEquals(320, concealed.length);
        assertTrue(rms(concealed) > 0.0);
    }

    @Test
    @DisplayName("throws after close")
    void closed() {
        var decoder = new ComfortNoiseDecoder(16000, 1);
        decoder.close();
        assertThrows(IllegalStateException.class, () -> decoder.conceal(320));
    }

    private static double rms(short[] samples) {
        var sum = 0.0;
        for (var sample : samples) {
            sum += (double) sample * sample;
        }
        return Math.sqrt(sum / samples.length);
    }
}
