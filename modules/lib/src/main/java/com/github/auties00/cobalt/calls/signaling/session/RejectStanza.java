package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <reject>} signal: the callee declines the call before answering.
 *
 * <p>A reject is sent by the callee in response to an inbound offer the user declines, or by the
 * engine when it refuses an offer it cannot service. It carries the universal call header, a
 * {@code reason} literal describing why the call was declined, and an optional fanout {@code count}.
 * The reason draws from the same vocabulary the terminate signal uses; it is modeled as a
 * {@link CallEndReason} with the original wire literal retained so an unrecognized literal still
 * round trips unchanged.
 *
 * <p>On the wire the element carries the call header attributes plus the reject specific ones:
 * {@snippet lang = xml :
 * <reject call-id="..." call-creator="..." reason="declined" count="2"/>
 *}
 *
 * @implNote This implementation classifies the wire {@code reason} literal into a
 * {@link CallEndReason} through {@link CallEndReason#fromWireValue(String)}, which collapses an
 * unrecognized literal to {@link CallEndReason#UNKNOWN}. The raw literal is stored separately in
 * {@link #reasonWire} so it re emits byte for byte regardless of whether it was recognized.
 * @see SignalingType#REJECT
 * @see CallEndReason
 */
public final class RejectStanza implements CallMessage {
    /**
     * The wire element tag for a reject signal.
     */
    public static final String ELEMENT = "reject";

    /**
     * The wire attribute naming the decline reason.
     */
    private static final String REASON_ATTRIBUTE = "reason";

    /**
     * The wire attribute naming the fanout count.
     */
    private static final String COUNT_ATTRIBUTE = "count";

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * The decline reason; never {@code null}.
     */
    private final CallEndReason reason;

    /**
     * The exact wire {@code reason} literal, retained so an unrecognized reason emits verbatim;
     * never {@code null}.
     */
    private final String reasonWire;

    /**
     * The fanout count, or {@code -1} when absent.
     */
    private final int count;

    /**
     * Constructs a reject signal.
     *
     * @param callId      the call identifier; never {@code null}
     * @param callCreator the call creator's device JID; never {@code null}
     * @param reason      the decline reason; never {@code null}
     * @param reasonWire  the exact wire {@code reason} literal, retained so an unrecognized reason
     *                    emits verbatim; never {@code null}
     * @param count       the fanout count, or {@code -1} when absent
     * @throws NullPointerException if {@code callId}, {@code callCreator}, {@code reason}, or
     *                              {@code reasonWire} is {@code null}
     */
    public RejectStanza(String callId, Jid callCreator, CallEndReason reason, String reasonWire, int count) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.reasonWire = Objects.requireNonNull(reasonWire, "reasonWire cannot be null");
        this.count = count;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a reject
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a reject
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the decline reason.
     *
     * @return the decline reason; never {@code null}
     */
    public CallEndReason reason() {
        return reason;
    }

    /**
     * Returns the exact wire {@code reason} literal.
     *
     * @return the wire reason literal; never {@code null}
     */
    public String reasonWire() {
        return reasonWire;
    }

    /**
     * Returns a reject carrying a typed reason, with no fanout count, deriving the wire literal from
     * the reason.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param reason      the decline reason
     * @return the reject signal
     * @throws NullPointerException if any argument is {@code null}
     */
    public static RejectStanza of(String callId, Jid callCreator, CallEndReason reason) {
        Objects.requireNonNull(reason, "reason cannot be null");
        return new RejectStanza(callId, callCreator, reason, reason.wireValue(), -1);
    }

    /**
     * Returns the fanout count, if present.
     *
     * @return an {@link OptionalInt} holding the count, or empty when the signal carries none
     */
    public OptionalInt countValue() {
        return count < 0 ? OptionalInt.empty() : OptionalInt.of(count);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#REJECT}
     */
    @Override
    public SignalingType type() {
        return SignalingType.REJECT;
    }

    /**
     * Builds the {@code <reject call-id call-creator reason count/>} action stanza.
     *
     * <p>An absent {@code count} is omitted from the stanza.
     *
     * @return the reject action stanza
     */
    @Override
    public Stanza toStanza() {
        return CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(REASON_ATTRIBUTE, reasonWire)
                .attribute(COUNT_ATTRIBUTE, count, count >= 0)
                .build();
    }

    /**
     * Decodes a {@code <reject>} action stanza into a {@link RejectStanza}.
     *
     * <p>The {@code reason} literal is retained verbatim and also classified into a
     * {@link CallEndReason}; an absent {@code reason} classifies to {@link CallEndReason#UNKNOWN} with
     * an empty wire literal.
     *
     * @param stanza the {@code <reject>} stanza
     * @return the decoded reject signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static RejectStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var reasonWire = stanza.getAttributeAsString(REASON_ATTRIBUTE, "");
        var reason = CallEndReason.fromWireValue(reasonWire);
        var count = stanza.getAttributeAsInt(COUNT_ATTRIBUTE, -1);
        return new RejectStanza(callId, callCreator, reason, reasonWire, count);
    }

    /**
     * Returns whether {@code obj} is a {@link RejectStanza} with equal fields.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a reject with every field equal
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RejectStanza that
                && count == that.count
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && reason.equals(that.reason)
                && reasonWire.equals(that.reasonWire));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this reject
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, reason, reasonWire, count);
    }

    /**
     * Returns a string describing this reject and its fields for diagnostics.
     *
     * @return the diagnostic string
     */
    @Override
    public String toString() {
        return "RejectStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", reason=" + reason
                + ", reasonWire=" + reasonWire
                + ", count=" + count + ']';
    }
}
