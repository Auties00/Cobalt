package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents the {@code <encopt>} encryption options child of an offer, accept, or preaccept.
 *
 * <p>The encryption options element selects the key generation scheme for the call's media keys. Its
 * single {@code keygen} attribute names the scheme version; the value {@code 2} is the standard key
 * generation version every observed revision uses, and the participant crypto chain accepts only a
 * {@code keygen} equal to {@code 2}. This record models the element as it appears on the wire:
 *
 * {@snippet lang = xml:
 * <encopt keygen="2"/>
 *}
 *
 * @param keygen the key generation scheme version
 * @see SignalingType#OFFER
 */
public record CallEncOptions(int keygen) {
    /**
     * The wire element tag for the encryption options child.
     */
    public static final String ELEMENT = "encopt";

    /**
     * The wire attribute naming the key generation scheme version.
     */
    private static final String KEYGEN_ATTRIBUTE = "keygen";

    /**
     * The standard key generation scheme version accepted by the participant crypto chain.
     */
    public static final int DEFAULT_KEYGEN = 2;

    /**
     * Returns the encryption options carrying the {@linkplain #DEFAULT_KEYGEN standard} key generation
     * version.
     *
     * @return the default encryption options
     */
    public static CallEncOptions standard() {
        return new CallEncOptions(DEFAULT_KEYGEN);
    }

    /**
     * Builds the {@code <encopt>} stanza for these options.
     *
     * <p>The resulting stanza carries a single {@link #KEYGEN_ATTRIBUTE} attribute set to {@link #keygen()}.
     *
     * @return the encryption options stanza
     */
    public Stanza toStanza() {
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(KEYGEN_ATTRIBUTE, keygen)
                .build();
    }

    /**
     * Decodes an {@code <encopt>} stanza into a {@link CallEncOptions}.
     *
     * <p>A {@code null} stanza, or one whose description is not {@link #ELEMENT}, yields an empty result so
     * callers iterating a mixed child list can skip it. An {@code <encopt>} with no {@code keygen}
     * attribute decodes to the {@linkplain #DEFAULT_KEYGEN standard} version.
     *
     * @param stanza the {@code <encopt>} stanza
     * @return the decoded options, or an empty result when the stanza is not an {@code <encopt>} element
     */
    public static Optional<CallEncOptions> of(Stanza stanza) {
        if (stanza == null || !stanza.hasDescription(ELEMENT)) {
            return Optional.empty();
        }
        return Optional.of(new CallEncOptions(stanza.getAttributeAsInt(KEYGEN_ATTRIBUTE, DEFAULT_KEYGEN)));
    }
}
