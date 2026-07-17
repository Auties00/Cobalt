package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents the {@code <media>} descriptor carried by an offer, accept, or preaccept.
 *
 * <p>The media descriptor pins the negotiated audio sample rate and an encryption scheme selector
 * for the call's media plane. The {@code rate} attribute is the audio sample rate in hertz, drawn
 * from the fixed set {@code 8000}, {@code 16000}, {@code 24000}, {@code 48000}; the {@code enc}
 * attribute is the engine's encryption scheme bitmask. This record carries both as they appear on
 * the wire, using {@code -1} for either value when its attribute is absent.
 *
 * <p>On the wire the element is {@code <media enc="N" rate="R"/>}. The engine maps each recognized
 * sample rate to an internal flag through the fixed table {@code 8000 -> 1}, {@code 16000 -> 2},
 * {@code 24000 -> 4}, {@code 48000 -> 8}; {@link #rateFlag()} exposes that mapping. Some shapes of
 * {@code <media>} additionally nest an {@code <enc>} key child, which the offer and accept records
 * carry separately rather than inside this descriptor.
 *
 * @param enc        the encryption scheme bitmask, or {@code -1} when the descriptor carries no
 *                   {@code enc} attribute
 * @param sampleRate the audio sample rate in hertz, or {@code -1} when the descriptor carries no
 *                   {@code rate} attribute
 * @see CallCodecDescriptor
 */
public record CallMediaDescriptor(int enc, int sampleRate) {
    /**
     * The wire element tag for a media descriptor.
     */
    public static final String ELEMENT = "media";

    /**
     * The wire attribute naming the encryption scheme bitmask.
     */
    private static final String ENC_ATTRIBUTE = "enc";

    /**
     * The wire attribute naming the audio sample rate.
     */
    private static final String RATE_ATTRIBUTE = "rate";

    /**
     * Returns the encryption scheme bitmask, if present.
     *
     * <p>The bitmask is present when {@link #enc()} is non negative; a value of {@code -1} means the
     * descriptor carried no {@code enc} attribute and yields an empty result.
     *
     * @return an {@link OptionalInt} holding the {@code enc} value, or empty when absent
     */
    public OptionalInt encValue() {
        return enc < 0 ? OptionalInt.empty() : OptionalInt.of(enc);
    }

    /**
     * Returns the audio sample rate in hertz, if present.
     *
     * <p>The rate is present when {@link #sampleRate()} is non negative; a value of {@code -1} means
     * the descriptor carried no {@code rate} attribute and yields an empty result.
     *
     * @return an {@link OptionalInt} holding the sample rate, or empty when absent
     */
    public OptionalInt sampleRateValue() {
        return sampleRate < 0 ? OptionalInt.empty() : OptionalInt.of(sampleRate);
    }

    /**
     * Returns the internal sample rate flag the engine assigns to this descriptor's
     * {@link #sampleRate() rate}.
     *
     * <p>The mapping is the fixed table {@code 8000 -> 1}, {@code 16000 -> 2}, {@code 24000 -> 4},
     * {@code 48000 -> 8}; a rate outside that set, or an absent rate, yields an empty result.
     *
     * @return an {@link OptionalInt} holding the rate flag, or empty when the rate is unrecognized or
     *         absent
     */
    public OptionalInt rateFlag() {
        return switch (sampleRate) {
            case 8000 -> OptionalInt.of(1);
            case 16000 -> OptionalInt.of(2);
            case 24000 -> OptionalInt.of(4);
            case 48000 -> OptionalInt.of(8);
            default -> OptionalInt.empty();
        };
    }

    /**
     * Builds the {@code <media enc=... rate=.../>} stanza for this descriptor.
     *
     * <p>An absent {@code enc} or {@code rate}, signalled by a negative value, is omitted from the
     * stanza rather than written as a sentinel, so the round trip through {@link #of(Stanza)} yields
     * back {@code -1} for the missing attribute.
     *
     * @return the media descriptor stanza
     */
    public Stanza toStanza() {
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(ENC_ATTRIBUTE, enc, enc >= 0)
                .attribute(RATE_ATTRIBUTE, sampleRate, sampleRate >= 0)
                .build();
    }

    /**
     * Decodes a {@code <media>} stanza into a {@link CallMediaDescriptor}.
     *
     * <p>A {@code null} stanza, or one whose element is not {@code <media>}, yields an empty result so
     * callers iterating a mixed child list can skip it. A missing {@code enc} or {@code rate}
     * attribute decodes to {@code -1} in the corresponding record component.
     *
     * @param stanza the {@code <media>} stanza
     * @return the decoded descriptor, or an empty result when the stanza is {@code null} or is not a
     *         {@code <media>} element
     */
    public static Optional<CallMediaDescriptor> of(Stanza stanza) {
        if (stanza == null || !stanza.hasDescription(ELEMENT)) {
            return Optional.empty();
        }
        var enc = stanza.getAttributeAsInt(ENC_ATTRIBUTE, -1);
        var sampleRate = stanza.getAttributeAsInt(RATE_ATTRIBUTE, -1);
        return Optional.of(new CallMediaDescriptor(enc, sampleRate));
    }
}
