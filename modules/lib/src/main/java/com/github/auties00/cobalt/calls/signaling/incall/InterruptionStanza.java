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
 * Represents an {@code <interruption>} in call action, signalling that a peer's media stream has been
 * interrupted or has recovered.
 *
 * <p>An interruption signals that a participant's audio or video can no longer flow, or that a prior
 * interruption has cleared. It carries the common call header, a {@code state} flag that is one of the
 * two literals {@code begin} (the interruption started) or {@code end} (it cleared), and a numeric
 * {@code type} code classifying the cause of the interruption. The engine surfaces these to the host as
 * an interruption state change event and may play the interruption tone while a {@code begin} is
 * outstanding.
 *
 * <p>On the wire the element carries the header attributes plus the interruption phase and cause:
 * {@snippet lang="xml" :
 * <interruption call-id="..." call-creator="..." state="begin|end" type="N"/>
 * }
 *
 * @see SignalingType#INTERRUPTION
 */
public final class InterruptionStanza implements InCallActionStanza {
    /**
     * The wire element tag for an interruption action.
     */
    public static final String ELEMENT = "interruption";

    /**
     * The wire attribute naming the interruption phase.
     */
    private static final String STATE_ATTRIBUTE = "state";

    /**
     * The wire attribute naming the code classifying the cause of the interruption.
     */
    private static final String TYPE_ATTRIBUTE = "type";

    /**
     * The {@code state} literal indicating an interruption is starting.
     */
    private static final String STATE_BEGIN = "begin";

    /**
     * The {@code state} literal indicating an interruption is clearing.
     */
    private static final String STATE_END = "end";

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * {@code true} when the interruption is starting ({@code state="begin"}), {@code false} when it is
     * clearing ({@code state="end"}).
     */
    private final boolean began;

    /**
     * The numeric code classifying the cause of the interruption, carried in the wire {@code type}
     * attribute.
     */
    private final int interruptionType;

    /**
     * Constructs an interruption action, validating the components.
     *
     * @param callId           the call identifier; never {@code null}
     * @param callCreator      the call creator's device JID; never {@code null}
     * @param began            {@code true} when the interruption is starting ({@code state="begin"}),
     *                         {@code false} when it is clearing ({@code state="end"})
     * @param interruptionType the numeric code classifying the cause of the interruption, carried in
     *                         the wire {@code type} attribute
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public InterruptionStanza(String callId, Jid callCreator, boolean began, int interruptionType) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.began = began;
        this.interruptionType = interruptionType;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for an interruption
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for an interruption
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns whether the interruption is starting rather than clearing.
     *
     * @return {@code true} when the interruption is starting ({@code state="begin"}), {@code false}
     *         when it is clearing ({@code state="end"})
     */
    public boolean began() {
        return began;
    }

    /**
     * Returns the numeric code classifying the cause of the interruption, carried in the wire
     * {@code type} attribute.
     *
     * @return the code classifying the cause of the interruption
     */
    public int interruptionType() {
        return interruptionType;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#INTERRUPTION}
     */
    @Override
    public SignalingType type() {
        return SignalingType.INTERRUPTION;
    }

    /**
     * Builds the {@code <interruption call-id call-creator state type/>} action stanza.
     *
     * <p>The {@code state} attribute serializes to {@code begin} when {@link #began()} is
     * {@code true} and {@code end} otherwise.
     *
     * @return the interruption action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(STATE_ATTRIBUTE, began ? STATE_BEGIN : STATE_END)
                .attribute(TYPE_ATTRIBUTE, interruptionType)
                .build();
    }

    /**
     * Decodes an {@code <interruption>} action stanza into an {@link InterruptionStanza}.
     *
     * <p>The {@code state} attribute is interpreted as {@link #began()} {@code true} only when it
     * equals the literal {@code begin}; any other value, including an absent attribute, decodes as
     * {@code false}. An absent {@code type} decodes to {@code 0}.
     *
     * @param stanza the {@code <interruption>} stanza
     * @return the decoded interruption action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static InterruptionStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var began = STATE_BEGIN.equals(stanza.getAttributeAsString(STATE_ATTRIBUTE, STATE_END));
        var interruptionType = stanza.getAttributeAsInt(TYPE_ATTRIBUTE, 0);
        return new InterruptionStanza(callId, callCreator, began, interruptionType);
    }

    /**
     * Returns whether {@code obj} is an {@link InterruptionStanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an interruption action with the same field values
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof InterruptionStanza that
                && began == that.began
                && interruptionType == that.interruptionType
                && Objects.equals(callId, that.callId)
                && Objects.equals(callCreator, that.callCreator));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this interruption action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, began, interruptionType);
    }

    /**
     * Returns a string representation of this interruption action for diagnostics.
     *
     * @return the diagnostic string
     */
    @Override
    public String toString() {
        return "InterruptionStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", began=" + began
                + ", interruptionType=" + interruptionType
                + ']';
    }
}
