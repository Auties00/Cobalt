package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <terminate>} signal by which either side ends the call.
 *
 * <p>A terminate ends an in progress or ringing call and carries the reason it ended. Beyond the
 * universal call header it carries a reason literal (written on the {@code reason} attribute), an
 * optional fanout {@code count}, a first wave marker, an optional terminate transaction id, an
 * optional terminate hint, an optional {@code <call_summary>} block describing the ended call's
 * participants and durations, and an optional {@code <destination>} fanout of the peer device JIDs
 * the terminate must reach.
 *
 * <p>The reason uses the wire literal vocabulary shared with reject, classified into a
 * {@link CallEndReason} with the original literal retained. The {@code <call_summary>} block's
 * internal nesting is held as a raw {@link Stanza} subtree; callers that need its contents walk the
 * subtree directly.
 *
 * <p>On the wire the element is {@snippet lang="xml" :
 * <terminate call-id="..." call-creator="..." reason="<literal>" count="N"
 *            is_first_wave="1" terminate_transaction_id="N" terminate_hint="...">
 *   <call_summary>...</call_summary>
 *   <destination><to jid="<deviceJid>"/>...</destination>
 * </terminate>
 * }
 *
 * @see SignalingType#TERMINATE
 * @see CallEndReason
 */
public final class TerminateStanza implements CallMessage {
    /**
     * The wire element tag for a terminate signal.
     */
    public static final String ELEMENT = "terminate";

    /**
     * The wire attribute naming the end reason; every captured terminate carries the reason here.
     */
    private static final String REASON_ATTRIBUTE = "reason";

    /**
     * An alternate inbound spelling of the end reason attribute, accepted on decode for compatibility
     * but never emitted; the wire send uses {@link #REASON_ATTRIBUTE}.
     */
    private static final String TERMINATE_REASON_ATTRIBUTE = "terminate_reason";

    /**
     * The wire attribute naming the fanout count.
     */
    private static final String COUNT_ATTRIBUTE = "count";

    /**
     * The wire attribute marking the first terminate wave of a fanout.
     */
    private static final String FIRST_WAVE_ATTRIBUTE = "is_first_wave";

    /**
     * The wire attribute naming the terminate transaction id.
     */
    private static final String TRANSACTION_ID_ATTRIBUTE = "terminate_transaction_id";

    /**
     * The wire attribute naming the terminate hint.
     */
    private static final String HINT_ATTRIBUTE = "terminate_hint";

    /**
     * The wire element tag for the call summary block.
     */
    private static final String CALL_SUMMARY_ELEMENT = "call_summary";

    /**
     * The wire element tag for the fanout destination block.
     */
    private static final String DESTINATION_ELEMENT = "destination";

    /**
     * The wire element tag for one fanout target inside {@code <destination>}.
     */
    private static final String TO_ELEMENT = "to";

    /**
     * The wire attribute naming the target device JID on a {@code <to>} element.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * The wire literal a boolean attribute carries when set; booleans on the call plane serialize as
     * {@code '1'}/{@code '0'} rather than {@code true}/{@code false}.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The wire literal a boolean attribute carries when clear.
     */
    private static final String FLAG_FALSE = "0";

    /**
     * The call identifier; never {@code null}.
     */
    private final String callId;

    /**
     * The call creator's device JID; never {@code null}.
     */
    private final Jid callCreator;

    /**
     * The end reason; never {@code null}.
     */
    private final CallEndReason reason;

    /**
     * The exact wire reason literal, retained to be emitted again verbatim; never {@code null}.
     */
    private final String reasonWire;

    /**
     * The fanout count, or {@code -1} when absent.
     */
    private final int count;

    /**
     * Whether this is the first terminate wave of a fanout.
     */
    private final boolean firstWave;

    /**
     * The terminate transaction id, or {@code -1} when absent.
     */
    private final int transactionId;

    /**
     * The terminate hint, or {@code null} when absent.
     */
    private final String hint;

    /**
     * The parsed {@code <call_summary>} of the ended call, or {@code null} when absent.
     */
    private final CallSummary callSummary;

    /**
     * The peer device JIDs the terminate fans out to; never {@code null}, possibly empty.
     */
    private final List<Jid> destination;

    /**
     * Constructs a terminate signal, copying the destination list.
     *
     * @param callId        the call identifier; never {@code null}
     * @param callCreator   the call creator's device JID; never {@code null}
     * @param reason        the end reason; never {@code null}
     * @param reasonWire    the exact wire reason literal, retained to be emitted again verbatim; never
     *                      {@code null}
     * @param count         the fanout count, or {@code -1} when absent
     * @param firstWave     whether this is the first terminate wave of a fanout
     * @param transactionId the terminate transaction id, or {@code -1} when absent
     * @param hint          the terminate hint, or {@code null} when absent
     * @param callSummary   the parsed {@code <call_summary>} of the ended call, or {@code null} when absent
     * @param destination   the peer device JIDs the terminate fans out to; never {@code null}, possibly
     *                      empty
     * @throws NullPointerException if {@code callId}, {@code callCreator}, {@code reason},
     *                              {@code reasonWire}, or {@code destination} is {@code null}, or if
     *                              {@code destination} contains a {@code null} element
     */
    public TerminateStanza(String callId, Jid callCreator, CallEndReason reason, String reasonWire, int count,
                           boolean firstWave, int transactionId, String hint, CallSummary callSummary,
                           List<Jid> destination) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.reasonWire = Objects.requireNonNull(reasonWire, "reasonWire cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        this.count = count;
        this.firstWave = firstWave;
        this.transactionId = transactionId;
        this.hint = hint;
        this.callSummary = callSummary;
        this.destination = List.copyOf(destination);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a terminate
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a terminate
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the end reason.
     *
     * @return the end reason; never {@code null}
     */
    public CallEndReason reason() {
        return reason;
    }

    /**
     * Returns the exact wire reason literal.
     *
     * @return the wire reason literal; never {@code null}
     */
    public String reasonWire() {
        return reasonWire;
    }

    /**
     * Returns whether this is the first terminate wave of a fanout.
     *
     * @return {@code true} when this is the first terminate wave
     */
    public boolean firstWave() {
        return firstWave;
    }

    /**
     * Returns the peer device JIDs the terminate fans out to.
     *
     * @return the fanout device JIDs; never {@code null}, possibly empty
     */
    public List<Jid> destination() {
        return destination;
    }

    /**
     * Returns a terminate carrying a typed reason and an optional fanout, deriving the wire literal
     * from the reason and carrying no summary, hint, transaction id, or first wave marker.
     *
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param reason      the end reason
     * @param destination the peer device JIDs to fan out to, or empty for none
     * @return the terminate signal
     * @throws NullPointerException if any argument is {@code null}, or if {@code destination} contains
     *                              a {@code null} element
     */
    public static TerminateStanza of(String callId, Jid callCreator, CallEndReason reason, List<Jid> destination) {
        Objects.requireNonNull(reason, "reason cannot be null");
        return new TerminateStanza(callId, callCreator, reason, reason.wireValue(), -1, false, -1, null, null,
                destination);
    }

    /**
     * Returns the fanout count, if present.
     *
     * @return an {@link OptionalInt} holding the count, or empty when absent
     */
    public OptionalInt countValue() {
        return count < 0 ? OptionalInt.empty() : OptionalInt.of(count);
    }

    /**
     * Returns the terminate transaction id, if present.
     *
     * @return an {@link OptionalInt} holding the transaction id, or empty when absent
     */
    public OptionalInt transactionIdValue() {
        return transactionId < 0 ? OptionalInt.empty() : OptionalInt.of(transactionId);
    }

    /**
     * Returns the terminate hint, if present.
     *
     * @return an {@link Optional} holding the hint, or empty when absent
     */
    public Optional<String> hintValue() {
        return Optional.ofNullable(hint);
    }

    /**
     * Returns the parsed {@code <call_summary>} of the ended call, if present.
     *
     * @return an {@link Optional} holding the call summary, or empty when absent
     */
    public Optional<CallSummary> callSummary() {
        return Optional.ofNullable(callSummary);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#TERMINATE}
     */
    @Override
    public SignalingType type() {
        return SignalingType.TERMINATE;
    }

    /**
     * Builds the {@code <terminate>} action stanza with its reason, optional attributes, optional
     * {@code <call_summary>}, and optional {@code <destination>} fanout.
     *
     * <p>The reason is written on {@code reason}; absent optional attributes are omitted;
     * the {@code <destination>} block is emitted only when at least one fanout target is present.
     *
     * @return the terminate action stanza
     */
    @Override
    public Stanza toStanza() {
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(REASON_ATTRIBUTE, reasonWire)
                .attribute(COUNT_ATTRIBUTE, count, count >= 0)
                .attribute(FIRST_WAVE_ATTRIBUTE, FLAG_TRUE, firstWave)
                .attribute(TRANSACTION_ID_ATTRIBUTE, transactionId, transactionId >= 0)
                .attribute(HINT_ATTRIBUTE, hint);
        var children = new ArrayList<Stanza>(2);
        if (callSummary != null) {
            children.add(callSummary.toStanza());
        }
        if (!destination.isEmpty()) {
            var toNodes = new ArrayList<Stanza>(destination.size());
            for (var device : destination) {
                toNodes.add(new StanzaBuilder()
                        .description(TO_ELEMENT)
                        .attribute(JID_ATTRIBUTE, device)
                        .build());
            }
            children.add(new StanzaBuilder()
                    .description(DESTINATION_ELEMENT)
                    .content(toNodes)
                    .build());
        }
        if (!children.isEmpty()) {
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <terminate>} action stanza into a {@link TerminateStanza}.
     *
     * <p>The reason is read from {@code reason}, falling back to the alternate
     * {@code terminate_reason} spelling, retained verbatim, and classified into a
     * {@link CallEndReason}. The
     * {@code <call_summary>} subtree is parsed into a typed {@link CallSummary}, and the
     * {@code <destination>} block's {@code <to>} children supply the fanout device JIDs.
     *
     * @param stanza the {@code <terminate>} stanza
     * @return the decoded terminate signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static TerminateStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var reasonWire = stanza.getAttributeAsString(REASON_ATTRIBUTE,
                stanza.getAttributeAsString(TERMINATE_REASON_ATTRIBUTE, ""));
        var reason = CallEndReason.fromWireValue(reasonWire);
        var count = stanza.getAttributeAsInt(COUNT_ATTRIBUTE, -1);
        var firstWave = FLAG_TRUE.equals(stanza.getAttributeAsString(FIRST_WAVE_ATTRIBUTE, FLAG_FALSE));
        var transactionId = stanza.getAttributeAsInt(TRANSACTION_ID_ATTRIBUTE, -1);
        var hint = stanza.getAttributeAsString(HINT_ATTRIBUTE, null);
        var callSummary = stanza.getChild(CALL_SUMMARY_ELEMENT).flatMap(CallSummary::of).orElse(null);
        var destination = stanza.getChild(DESTINATION_ELEMENT)
                .stream()
                .flatMap(d -> d.streamChildren(TO_ELEMENT))
                .flatMap(to -> to.streamAttributeAsJid(JID_ATTRIBUTE))
                .toList();
        return new TerminateStanza(callId, callCreator, reason, reasonWire, count, firstWave, transactionId, hint,
                callSummary, destination);
    }

    /**
     * Returns whether {@code obj} is a {@link TerminateStanza} with equal fields.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a terminate equal by value
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof TerminateStanza that
                && count == that.count
                && firstWave == that.firstWave
                && transactionId == that.transactionId
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && reason.equals(that.reason)
                && reasonWire.equals(that.reasonWire)
                && Objects.equals(hint, that.hint)
                && Objects.equals(callSummary, that.callSummary)
                && destination.equals(that.destination));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this terminate
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, reason, reasonWire, count, firstWave, transactionId, hint,
                callSummary, destination);
    }

    /**
     * Returns a string representation of this terminate for debugging.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "TerminateStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", reason=" + reason
                + ", reasonWire=" + reasonWire
                + ", count=" + count
                + ", firstWave=" + firstWave
                + ", transactionId=" + transactionId
                + ", hint=" + hint
                + ", callSummary=" + callSummary
                + ", destination=" + destination + ']';
    }
}
