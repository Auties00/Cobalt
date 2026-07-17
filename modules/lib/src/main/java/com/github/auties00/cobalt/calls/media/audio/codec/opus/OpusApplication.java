package com.github.auties00.cobalt.calls.media.audio.codec.opus;

import com.github.auties00.cobalt.calls.media.audio.codec.opus.bindings.CobaltOpus;

/**
 * Selects one of the three libopus application modes the call encoder can be opened in, each trading
 * algorithmic delay against coding strategy.
 *
 * <p>The application mode is fixed at encoder open and is passed as the third argument to
 * {@code cobalt_opus_encoder_create}; it biases which internal layer libopus prefers (SILK, CELT, or
 * their hybrid) at a given bitrate, whether the aggressive transmit high pass filter runs, and how much
 * lookahead delay the encoder may introduce. The three constants correspond exactly to the
 * {@code OPUS_APPLICATION_*} integer codes defined by RFC 6716; WhatsApp's voice path selects
 * {@link #VOIP}.
 *
 * @implNote This implementation maps {@code VOIP} to {@code 2048}, {@code AUDIO} to {@code 2049}, and
 * {@code RESTRICTED_LOWDELAY} to {@code 2051}. The values are read back through the portable
 * {@link CobaltOpus} shim binding ({@code COBALT_OPUS_APPLICATION_*}, which mirror the linked libopus
 * {@code OPUS_APPLICATION_*} values) rather than hardcoded.
 */
public enum OpusApplication {
    /**
     * Favors voice intelligibility over fidelity; the mode WhatsApp's call engine opens.
     *
     * <p>This mode steers libopus toward the SILK layer at voice bitrates and enables the aggressive
     * transmit high pass filter that strips DC offset and low frequency rumble before coding.
     */
    VOIP,

    /**
     * Balances quality for mixed music and voice content.
     *
     * <p>This mode lets libopus spend more complexity on broadband fidelity and omits the aggressive
     * high pass filter {@link #VOIP} applies, at the cost of being less optimized for pure speech.
     */
    AUDIO,

    /**
     * Restricts libopus to its lowest delay modes for latency sensitive use.
     *
     * <p>This mode forbids the extra lookahead the other modes introduce; it is not used on the
     * WhatsApp call interoperability path.
     */
    RESTRICTED_LOWDELAY;

    /**
     * Returns the libopus {@code OPUS_APPLICATION_*} integer code for this mode.
     *
     * <p>The returned value is the native constant {@code cobalt_opus_encoder_create} accepts; each
     * constant resolves to exactly one {@code COBALT_OPUS_APPLICATION_*} code from the
     * {@link CobaltOpus} shim binding.
     *
     * @return the libopus application code corresponding to this constant
     */
    int toNative() {
        return switch (this) {
            case VOIP -> CobaltOpus.COBALT_OPUS_APPLICATION_VOIP();
            case AUDIO -> CobaltOpus.COBALT_OPUS_APPLICATION_AUDIO();
            case RESTRICTED_LOWDELAY -> CobaltOpus.COBALT_OPUS_APPLICATION_RESTRICTED_LOWDELAY();
        };
    }

    /**
     * Returns the application mode for the given libopus {@code OPUS_APPLICATION_*} code.
     *
     * @param code the native application code, one of {@code 2048}, {@code 2049}, {@code 2051}
     * @return the matching mode
     * @throws IllegalArgumentException if {@code code} is not a recognized application code
     */
    static OpusApplication ofNative(int code) {
        if (code == CobaltOpus.COBALT_OPUS_APPLICATION_VOIP()) {
            return VOIP;
        }
        if (code == CobaltOpus.COBALT_OPUS_APPLICATION_AUDIO()) {
            return AUDIO;
        }
        if (code == CobaltOpus.COBALT_OPUS_APPLICATION_RESTRICTED_LOWDELAY()) {
            return RESTRICTED_LOWDELAY;
        }
        throw new IllegalArgumentException("Unknown Opus application code: " + code);
    }
}
