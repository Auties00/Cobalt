package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <dtmf>} in call action carrying a single dual tone multi frequency keypress
 * during a business or PSTN call.
 *
 * <p>A DTMF action delivers one keypress to the far end of a business or gateway bridged call. It
 * carries the common call header ({@code call-id} plus {@code call-creator}) and a single
 * {@code <tone>} child element whose text content is the tone symbol: a digit {@code 0} through
 * {@code 9}, {@code *}, {@code #}, or a letter {@code A} through {@code D}. Each action carries
 * exactly one tone, so a sequence of keypresses is transmitted as a sequence of actions.
 *
 * <p>On the wire the element is
 * {@snippet lang="xml" :
 * <dtmf call-id="..." call-creator="...">
 *   <tone>5</tone>
 * </dtmf>
 * }
 *
 * @see SignalingType#DTMF_TONE
 */
public final class DtmfStanza implements InCallActionStanza {
    /**
     * The wire element tag for a DTMF action.
     */
    public static final String ELEMENT = "dtmf";

    /**
     * The wire child element carrying the DTMF tone symbol.
     */
    private static final String TONE_ELEMENT = "tone";

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * The DTMF tone symbol; never {@code null}.
     */
    private final String tone;

    /**
     * Constructs a DTMF action, validating the components.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param tone        the DTMF tone symbol; never {@code null}
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code tone} is
     *                              {@code null}
     */
    public DtmfStanza(String callId, Jid callCreator, String tone) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.tone = Objects.requireNonNull(tone, "tone cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a DTMF action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a DTMF action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the DTMF tone symbol.
     *
     * @return the tone symbol
     */
    public String tone() {
        return tone;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#DTMF_TONE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.DTMF_TONE;
    }

    /**
     * Builds the {@code <dtmf> <tone/> </dtmf>} action stanza.
     *
     * @return the DTMF action stanza
     */
    @Override
    public Stanza toStanza() {
        var toneNode = new StanzaBuilder()
                .description(TONE_ELEMENT)
                .content(tone)
                .build();
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .content(toneNode)
                .build();
    }

    /**
     * Decodes a {@code <dtmf>} action stanza into a {@link DtmfStanza}.
     *
     * @param stanza the {@code <dtmf>} stanza
     * @return the decoded DTMF action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent, or if the {@code <tone>} child element is absent
     */
    public static DtmfStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var tone = stanza.getChild(TONE_ELEMENT)
                .flatMap(Stanza::toContentString)
                .orElseThrow(() -> new NoSuchElementException("dtmf requires a tone child element"));
        return new DtmfStanza(callId, callCreator, tone);
    }

    /**
     * Returns whether {@code obj} is a {@link DtmfStanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal DTMF action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof DtmfStanza that
                && Objects.equals(callId, that.callId)
                && Objects.equals(callCreator, that.callCreator)
                && Objects.equals(tone, that.tone));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this DTMF action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, tone);
    }

    /**
     * Returns a debug string for this DTMF action.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "DtmfStanza[callId=" + callId + ", callCreator=" + callCreator + ", tone=" + tone + ']';
    }
}
