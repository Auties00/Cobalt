package com.github.auties00.cobalt.calls.media.audio.neteq.decoder;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DtmfDecoder RFC 4733 telephone-event")
class DtmfDecoderTest {
    private static byte[] event(int code, boolean end, int volume, int duration) {
        var payload = new byte[4];
        payload[0] = (byte) code;
        payload[1] = (byte) ((end ? 0x80 : 0) | (volume & 0x3F));
        payload[2] = (byte) ((duration >> 8) & 0xFF);
        payload[3] = (byte) (duration & 0xFF);
        return payload;
    }

    @Test
    @DisplayName("synthesizes a non-silent tone for an active event")
    void synthesizesTone() {
        var decoder = new DtmfDecoder(16000, 1);
        var frame = decoder.decode(event(5, false, 10, 160), 320, false);
        assertEquals(320, frame.length);
        var peak = 0;
        for (var sample : frame) {
            peak = Math.max(peak, Math.abs(sample));
        }
        assertTrue(peak > 0, "tone was silent");
    }

    @Test
    @DisplayName("renders silence for the end-of-event packet")
    void endIsSilent() {
        var decoder = new DtmfDecoder(16000, 1);
        var frame = decoder.decode(event(5, true, 10, 800), 320, false);
        for (var sample : frame) {
            assertEquals(0, sample);
        }
    }

    @Test
    @DisplayName("rejects a payload shorter than the event header")
    void shortPayload() {
        var decoder = new DtmfDecoder(16000, 1);
        assertThrows(WhatsAppCallException.Rtp.class, () -> decoder.decode(new byte[]{1, 2}, 320, false));
    }

    @Test
    @DisplayName("returns silence from conceal when no tone is active")
    void concealWithoutTone() {
        var decoder = new DtmfDecoder(16000, 1);
        var frame = decoder.conceal(320);
        for (var sample : frame) {
            assertEquals(0, sample);
        }
    }
}
