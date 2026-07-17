package com.github.auties00.cobalt.calls.signaling.link;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <link_join>} signal: a request to join a call through a call link token.
 *
 * <p>A link join request asks the relay to admit the local device into the call that a previously
 * resolved call link {@link #token() token} points at. It optionally carries a {@link #joinState() join
 * state} marking which leg of the two step join handshake this request represents. The relay answers on a
 * {@link LinkJoinAck}, which fills in the call id, the call creator, and the membership roster (and, for a
 * waiting room link, the lobby participant list) on success; the ack handler rejects a reply whose echoed
 * token does not match the request.
 *
 * <p>This control plane element models only the call link join addressing the relay needs to route the
 * join. The accompanying media negotiation payload (audio capabilities, the voip capability version, the
 * raw end to end key, and the video codec capability) is owned by the offer/accept and crypto units and is
 * not carried here.
 *
 * <p>On the wire the element is:
 * {@snippet lang="xml" :
 * <link_join token="..." join-state="N"/>
 * }
 *
 * @implNote This implementation writes a {@code join-state} value of {@code 1} or {@code 2} when a leg is
 * present, and omits the attribute otherwise.
 * @param token     the call link token to join; never {@code null}
 * @param joinState the two step join handshake leg, or {@code -1} when absent; the {@link #joinStateValue()}
 *                  accessor exposes the present or absent view
 * @see SignalingType#LINK_JOIN
 * @see LinkJoinAck
 */
public record LinkJoinStanza(String token, int joinState) implements CallMessage {
    /**
     * The wire element tag for a link join signal.
     */
    public static final String ELEMENT = "link_join";

    /**
     * The wire attribute naming the call link token.
     */
    private static final String TOKEN_ATTRIBUTE = "token";

    /**
     * The wire attribute naming the two step join handshake leg.
     */
    private static final String JOIN_STATE_ATTRIBUTE = "join-state";

    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code token} is {@code null}
     */
    public LinkJoinStanza {
        Objects.requireNonNull(token, "token cannot be null");
    }

    /**
     * Returns a link join signal carrying only the token, with no explicit {@code join-state} leg.
     *
     * @param token the call link token to join
     * @return the link join signal
     * @throws NullPointerException if {@code token} is {@code null}
     */
    public static LinkJoinStanza of(String token) {
        return new LinkJoinStanza(token, -1);
    }

    /**
     * Returns the two step join handshake leg as a present or absent value.
     *
     * <p>This is the companion to the canonical {@link #joinState()} accessor, which returns the raw
     * sentinel {@code -1} when no leg is set.
     *
     * @return an {@link OptionalInt} holding the {@code join-state} value, or empty when absent
     */
    public OptionalInt joinStateValue() {
        return joinState < 0 ? OptionalInt.empty() : OptionalInt.of(joinState);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a call link join is keyed by its link token, not a
     *         {@code call-id} header
     */
    @Override
    public Optional<String> callId() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a call link join carries no {@code call-creator} header
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.empty();
    }

    @Override
    public SignalingType type() {
        return SignalingType.LINK_JOIN;
    }

    /**
     * Builds the {@code <link_join token join-state/>} action stanza.
     *
     * <p>An absent join state is omitted from the stanza.
     *
     * @return the link join action stanza
     */
    @Override
    public Stanza toStanza() {
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(TOKEN_ATTRIBUTE, token)
                .attribute(JOIN_STATE_ATTRIBUTE, joinState, joinState >= 0)
                .build();
    }

    /**
     * Decodes a {@code <link_join>} action stanza into a {@link LinkJoinStanza}.
     *
     * <p>An absent {@code join-state} attribute classifies to an empty {@link #joinState()}.
     *
     * @param stanza the {@code <link_join>} stanza
     * @return the decoded link join signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code token} attribute is absent
     */
    public static LinkJoinStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var token = stanza.getRequiredAttributeAsString(TOKEN_ATTRIBUTE);
        var joinState = stanza.getAttributeAsInt(JOIN_STATE_ATTRIBUTE, -1);
        return new LinkJoinStanza(token, joinState);
    }
}
