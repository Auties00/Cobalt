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
 * Represents a {@code <raise_hand>} in call action by which a participant raises or lowers a hand in a
 * group call.
 *
 * <p>The action reports whether the sender currently has a hand raised. It carries the universal call
 * header, a {@code raise-hand-state} flag ({@code "1"} raised, {@code "0"} lowered), and an optional
 * {@code broadcast} flag set when the action is fanned out to every participant. The resulting state
 * feeds the grid ranking comparator, where participants with a hand raised sort first, and surfaces a
 * hand state change event to the host.
 *
 * <p>Unlike most in call actions {@code raise_hand} has no entry in the numeric
 * {@code voip_signaling_message_type} table; it is carried inside the {@link SignalingType#NOTIFY
 * notify} message container and dispatched on its wire tag, so {@link #type()} returns {@code null}.
 *
 * <p>On the wire the element is
 * {@snippet lang="xml" :
 * <raise_hand call-id="..." call-creator="..." raise-hand-state="1|0" broadcast="1"/>
 * }
 *
 * @implNote This implementation reads {@code raise-hand-state} first and falls back to the legacy
 * {@code hand-raise-state} spelling on inbound nodes, and always emits the canonical
 * {@code raise-hand-state} on outbound nodes.
 * @see SignalingType#NOTIFY
 */
public final class RaiseHandStanza implements InCallActionStanza {
    /**
     * The wire element tag for a raise hand action.
     */
    public static final String ELEMENT = "raise_hand";

    /**
     * The canonical wire attribute carrying the raise hand state.
     */
    private static final String RAISE_HAND_STATE_ATTRIBUTE = "raise-hand-state";

    /**
     * The legacy wire attribute carrying the raise hand state, accepted on inbound nodes.
     */
    private static final String HAND_RAISE_STATE_ATTRIBUTE = "hand-raise-state";

    /**
     * The wire attribute flagging a fanned out action.
     */
    private static final String BROADCAST_ATTRIBUTE = "broadcast";

    /**
     * The wire literal for a set ({@code true}) voip boolean flag.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The wire literal for a clear ({@code false}) voip boolean flag.
     */
    private static final String FLAG_FALSE = "0";

    /**
     * The call identifier carried in this action's {@code call-id} header.
     */
    private final String callId;

    /**
     * The call creator device JID carried in this action's {@code call-creator} header.
     */
    private final Jid callCreator;

    /**
     * Whether the hand is raised ({@code true}) or lowered ({@code false}).
     */
    private final boolean raised;

    /**
     * Whether the action is fanned out to all participants.
     */
    private final boolean broadcast;

    /**
     * Constructs a raise hand action.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param raised      {@code true} when the hand is raised, {@code false} when lowered
     * @param broadcast   {@code true} when the action is fanned out to all participants
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public RaiseHandStanza(String callId, Jid callCreator, boolean raised, boolean broadcast) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.callId = callId;
        this.callCreator = callCreator;
        this.raised = raised;
        this.broadcast = broadcast;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a raise hand action
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a raise hand action
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns whether the hand is raised.
     *
     * @return {@code true} when the hand is raised, {@code false} when lowered
     */
    public boolean raised() {
        return raised;
    }

    /**
     * Returns whether the action is fanned out to all participants.
     *
     * @return {@code true} when the action is broadcast to all participants
     */
    public boolean broadcast() {
        return broadcast;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A raise hand action has no entry in the numeric {@code voip_signaling_message_type} table, so
     * this projection has no {@link SignalingType} and the method returns {@code null}; the
     * element is dispatched on its {@link #ELEMENT wire tag} inside the {@link SignalingType#NOTIFY
     * notify} container instead.
     *
     * @return {@code null}, since a raise hand action carries no taxonomy ordinal
     */
    @Override
    public SignalingType type() {
        return null;
    }

    /**
     * Builds the {@code <raise_hand call-id call-creator raise-hand-state broadcast/>} action stanza.
     *
     * <p>The {@code raise-hand-state} attribute serializes to {@code 1} when {@link #raised()} is
     * {@code true} and {@code 0} otherwise; the {@code broadcast} attribute is omitted unless
     * {@link #broadcast()} is {@code true}.
     *
     * @return the raise hand action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(RAISE_HAND_STATE_ATTRIBUTE, raised ? FLAG_TRUE : FLAG_FALSE)
                .attribute(BROADCAST_ATTRIBUTE, FLAG_TRUE, broadcast)
                .build();
    }

    /**
     * Decodes a {@code <raise_hand>} action stanza into a {@link RaiseHandStanza}.
     *
     * <p>The raise hand state is read from {@code raise-hand-state}, falling back to the legacy
     * {@code hand-raise-state} spelling; the required state attribute is the literal {@code 1} or
     * {@code 0}. An absent {@code broadcast} decodes to {@code false}.
     *
     * @param stanza the {@code <raise_hand>} stanza
     * @return the decoded raise hand action
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent, or if neither {@code raise-hand-state} nor
     *                                {@code hand-raise-state} is present
     */
    public static RaiseHandStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var state = stanza.getAttributeAsString(RAISE_HAND_STATE_ATTRIBUTE)
                .or(() -> stanza.getAttributeAsString(HAND_RAISE_STATE_ATTRIBUTE))
                .orElseThrow(() -> new NoSuchElementException(
                        "raise_hand requires raise-hand-state or hand-raise-state"));
        var broadcast = FLAG_TRUE.equals(stanza.getAttributeAsString(BROADCAST_ATTRIBUTE, FLAG_FALSE));
        return new RaiseHandStanza(callId, callCreator, FLAG_TRUE.equals(state), broadcast);
    }

    /**
     * Returns whether {@code obj} is a {@link RaiseHandStanza} with the same call header and flags.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal raise hand action
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RaiseHandStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && raised == that.raised
                && broadcast == that.broadcast);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this raise hand action
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, raised, broadcast);
    }

    /**
     * Returns a diagnostic string representation of this raise hand action.
     *
     * @return the diagnostic string
     */
    @Override
    public String toString() {
        return "RaiseHandStanza[callId=" + callId + ", callCreator=" + callCreator
                + ", raised=" + raised + ", broadcast=" + broadcast + ']';
    }
}
