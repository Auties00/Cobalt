package com.github.auties00.cobalt.calls.media.audio.codec;

import java.util.Objects;

/**
 * Holds one encoded audio frame produced by an {@link AudioCodec}, with the classification flags and
 * the measured loudness the encode path derives from the captured block.
 *
 * <p>The {@link #payload()} is the codec packet bytes; {@link #voiceActive()} reports whether the
 * encoder judged the frame to carry speech (from the TOC and VAD inspection), {@link #discontinuous()}
 * reports whether the frame is a discontinuous transmission or comfort noise frame (a payload shorter
 * than the speech threshold), {@link #hasForwardErrorCorrection()} reports whether the packet embeds
 * an in band FEC (LBRR) copy of the previous frame, and {@link #levelDbov()} carries the frame's
 * loudness as a positive {@code -dBov} magnitude in {@code [0, 127]} ({@code 0} loudest, {@code 127}
 * silence) measured from the captured PCM before encoding. A frame may be both empty (zero length
 * payload, when DTX produced nothing) and discontinuous; an empty payload is represented by a zero
 * length array, not {@code null}.
 *
 * @param payload                   the encoded codec packet bytes; never {@code null}, possibly empty
 *                                  when DTX suppressed output
 * @param voiceActive               whether the encoder classified the frame as voice active
 * @param discontinuous             whether the frame is a DTX or comfort noise frame
 * @param hasForwardErrorCorrection whether the packet embeds an in band FEC (LBRR) copy of the
 *                                  previous frame
 * @param levelDbov                 the frame loudness as a positive {@code -dBov} magnitude in
 *                                  {@code [0, 127]}, where {@code 0} is the loudest possible signal
 *                                  and {@code 127} is silence
 * @implNote This implementation derives every classification flag from the encoder output rather than
 * from the raw PCM: the speech versus DTX split is decided at a three byte threshold (a payload of
 * fewer than three bytes is a DTX or comfort noise frame), the {@link #voiceActive()} flag comes from
 * the packet VAD inspection, and {@link #hasForwardErrorCorrection()} from the in band FEC presence
 * check. The {@link #levelDbov()} is the root mean square energy of the captured PCM mapped to
 * {@code -dBov} before the encode, feeding the audio level RTP extension history. The downstream
 * sender uses {@link #voiceActive()} for mixing, {@link #discontinuous()} for zero rate transmit
 * bookkeeping, and {@link #levelDbov()} for the per packet audio level extension, where the loudest
 * frame (the minimum {@code -dBov}) governs the group.
 */
public record EncodedAudioFrame(
        byte[] payload,
        boolean voiceActive,
        boolean discontinuous,
        boolean hasForwardErrorCorrection,
        int levelDbov
) {
    /**
     * The {@code -dBov} magnitude representing silence or a below floor signal.
     *
     * <p>Equal to {@code 127}, the quietest representable level; a frame with no measurable energy
     * carries this value, and it is also the default a frame not derived from PCM reports.
     */
    public static final int SILENCE_LEVEL = 127;

    /**
     * Validates that the payload is present and clamps the level into the representable range.
     *
     * <p>A negative level is raised to {@code 0} (loudest) and a level above {@link #SILENCE_LEVEL} is
     * lowered to it, so {@link #levelDbov()} always lies in {@code [0, 127]}.
     *
     * @throws NullPointerException if {@code payload} is {@code null}
     */
    public EncodedAudioFrame {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (levelDbov < 0) {
            levelDbov = 0;
        } else if (levelDbov > SILENCE_LEVEL) {
            levelDbov = SILENCE_LEVEL;
        }
    }

    /**
     * Returns whether this frame carries no encoded bytes.
     *
     * <p>An empty frame is produced when discontinuous transmission suppresses output entirely for a
     * silence frame; the sender skips transmitting it.
     *
     * @return {@code true} if the {@linkplain #payload() payload} is zero length
     */
    public boolean isEmpty() {
        return payload.length == 0;
    }
}
