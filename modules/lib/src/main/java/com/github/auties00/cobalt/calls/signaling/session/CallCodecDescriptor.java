package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents one media format advertisement an offer, accept, or preaccept carries: a flat
 * {@code <audio>} or {@code <video>} element.
 *
 * <p>The wire carries one such element per advertised format, directly under the call action with no
 * wrapping block. An audio format is {@code <audio enc="opus" rate="8000"/>}, holding the {@code enc}
 * codec identifier and the {@code rate} sampling clock in hertz. A video format is
 * {@code <video dec="H264" enc="h.264" device_orientation="0" screen_width="0" screen_height="0"/>},
 * holding the {@code dec} decode codec token, the {@code enc} encode codec name, and the current
 * device orientation and screen geometry (all zero before the camera starts). The offer carries the
 * full video form; the preaccept drops {@code enc} and the accept drops {@code enc} and the screen
 * geometry, so a video descriptor may carry no {@code enc}. The element tag in {@link #element()}
 * distinguishes the two families and is preserved across a round trip.
 *
 * @param element           the wire element tag, {@code audio} for an audio format or {@code video}
 *                          for a video format; never {@code null}
 * @param enc               the codec identifier carried on the {@code enc} attribute, for example
 *                          {@code opus} or {@code h.264}, or {@code null} when the format carries no
 *                          {@code enc} (the accept and preaccept video forms)
 * @param rate              the audio sampling clock in hertz, or {@code -1} when absent (video)
 * @param dec               the video decode codec token carried on the {@code dec} attribute, for
 *                          example {@code H264}, or {@code null} when absent (audio)
 * @param deviceOrientation the video device orientation classification, or {@code -1} when absent
 * @param screenWidth       the video screen width in pixels, or {@code -1} when absent
 * @param screenHeight      the video screen height in pixels, or {@code -1} when absent
 * @see SignalingType#OFFER
 */
public record CallCodecDescriptor(String element, String enc, int rate, String dec, int deviceOrientation,
                                  int screenWidth, int screenHeight) {
    /**
     * The wire element tag for an audio format advertisement.
     */
    public static final String AUDIO_ELEMENT = "audio";

    /**
     * The wire element tag for a video format advertisement.
     */
    public static final String VIDEO_ELEMENT = "video";

    /**
     * The wire attribute naming the codec identifier, shared by audio and video formats.
     */
    private static final String ENC_ATTRIBUTE = "enc";

    /**
     * The wire attribute naming the audio sampling clock in hertz.
     */
    private static final String RATE_ATTRIBUTE = "rate";

    /**
     * The wire attribute naming the video decode codec token.
     */
    private static final String DEC_ATTRIBUTE = "dec";

    /**
     * The wire attribute naming the video device orientation classification.
     */
    private static final String DEVICE_ORIENTATION_ATTRIBUTE = "device_orientation";

    /**
     * The wire attribute naming the video screen width in pixels.
     */
    private static final String SCREEN_WIDTH_ATTRIBUTE = "screen_width";

    /**
     * The wire attribute naming the video screen height in pixels.
     */
    private static final String SCREEN_HEIGHT_ATTRIBUTE = "screen_height";

    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code element} is {@code null}
     */
    public CallCodecDescriptor {
        Objects.requireNonNull(element, "element cannot be null");
    }

    /**
     * Returns an audio format descriptor emitted as a flat {@code <audio enc rate/>} element.
     *
     * @param enc  the codec identifier, for example {@code opus}
     * @param rate the sampling clock in hertz
     * @return the audio format descriptor
     * @throws NullPointerException if {@code enc} is {@code null}
     */
    public static CallCodecDescriptor audio(String enc, int rate) {
        Objects.requireNonNull(enc, "enc cannot be null");
        return new CallCodecDescriptor(AUDIO_ELEMENT, enc, rate, null, -1, -1, -1);
    }

    /**
     * Returns a video format descriptor emitted as a flat
     * {@code <video dec enc device_orientation screen_width screen_height/>} element.
     *
     * @param dec               the decode codec token, for example {@code H264}
     * @param enc               the encode codec name, for example {@code h.264}, or {@code null} when the
     *                          format carries no {@code enc} (the accept and preaccept video forms)
     * @param deviceOrientation the device orientation classification
     * @param screenWidth       the screen width in pixels
     * @param screenHeight      the screen height in pixels
     * @return the video format descriptor
     * @throws NullPointerException if {@code dec} or {@code enc} is {@code null}
     */
    public static CallCodecDescriptor video(String dec, String enc, int deviceOrientation, int screenWidth,
                                            int screenHeight) {
        Objects.requireNonNull(dec, "dec cannot be null");
        return new CallCodecDescriptor(VIDEO_ELEMENT, enc, -1, dec, deviceOrientation, screenWidth, screenHeight);
    }

    /**
     * Returns the audio sampling clock in hertz, if present.
     *
     * @return an {@link OptionalInt} holding the sampling clock, or empty when the descriptor carries
     *         none
     */
    public OptionalInt rateValue() {
        return rate < 0 ? OptionalInt.empty() : OptionalInt.of(rate);
    }

    /**
     * Returns the video decode codec token, if present.
     *
     * @return an {@link Optional} holding the decode codec token, or empty when absent
     */
    public Optional<String> decValue() {
        return Optional.ofNullable(dec);
    }

    /**
     * Builds the codec format stanza, a flat {@code <audio>} or {@code <video>} element carrying the
     * format attributes.
     *
     * <p>Absent numeric values and the absent decode token are omitted from the stanza rather than
     * written as sentinels, so an audio format emits only {@code enc} and {@code rate} while a video
     * format emits {@code dec}, {@code enc}, and the geometry attributes.
     *
     * @return the codec format stanza
     */
    public Stanza toStanza() {
        return new StanzaBuilder()
                .description(element)
                .attribute(DEC_ATTRIBUTE, dec)
                .attribute(ENC_ATTRIBUTE, enc)
                .attribute(RATE_ATTRIBUTE, rate, rate >= 0)
                .attribute(DEVICE_ORIENTATION_ATTRIBUTE, deviceOrientation, deviceOrientation >= 0)
                .attribute(SCREEN_WIDTH_ATTRIBUTE, screenWidth, screenWidth >= 0)
                .attribute(SCREEN_HEIGHT_ATTRIBUTE, screenHeight, screenHeight >= 0)
                .build();
    }

    /**
     * Decodes a flat {@code <audio>} or {@code <video>} stanza into a {@link CallCodecDescriptor}.
     *
     * <p>The stanza's tag is retained as the descriptor {@link #element()} so a round trip preserves
     * whether it was an audio or video format. A stanza without an {@code enc} or {@code dec} attribute
     * yields an empty result so callers iterating a mixed child list can skip it.
     *
     * @param stanza the codec format stanza
     * @return the decoded descriptor, or an empty result when the stanza carries no {@code enc}
     *         attribute
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<CallCodecDescriptor> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var enc = stanza.getAttributeAsString(ENC_ATTRIBUTE, null);
        var dec = stanza.getAttributeAsString(DEC_ATTRIBUTE, null);
        if (enc == null && dec == null) {
            return Optional.empty();
        }
        var rate = stanza.getAttributeAsInt(RATE_ATTRIBUTE, -1);
        var deviceOrientation = stanza.getAttributeAsInt(DEVICE_ORIENTATION_ATTRIBUTE, -1);
        var screenWidth = stanza.getAttributeAsInt(SCREEN_WIDTH_ATTRIBUTE, -1);
        var screenHeight = stanza.getAttributeAsInt(SCREEN_HEIGHT_ATTRIBUTE, -1);
        return Optional.of(new CallCodecDescriptor(stanza.description(), enc, rate, dec, deviceOrientation,
                screenWidth, screenHeight));
    }
}
