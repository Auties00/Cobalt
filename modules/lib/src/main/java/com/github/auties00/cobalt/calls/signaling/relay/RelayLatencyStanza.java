package com.github.auties00.cobalt.calls.signaling.relay;

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
 * Represents a {@code <relaylatency>} signaling message, a relay latency probe report.
 *
 * <p>A relay latency message reports the round trip latency a device measured to each relay it
 * probed, together with the device's bandwidth estimate toward each relay. The body is a list of
 * {@link RelayLatencyEntry} entries, one per probed relay; the receiving side feeds them into relay
 * election and one side bandwidth estimation. The message carries the universal call header, may
 * carry a {@code transaction-id} correlating the report to a probe round, and may carry the
 * {@code has-bot} marker for a call that involves a bot.
 *
 * <p>This message is distinct from the {@code <relay>} block: a {@code <relay>} block advertises
 * relay servers and is parsed into {@link RelayInfo} and the {@link RelayCandidate} list, whereas a
 * {@code <relaylatency>} message reports measurements against those relays.
 *
 * <p>On the wire the message is
 * {@snippet lang="xml" :
 * <relaylatency call-id="..." call-creator="..." transaction-id="3">
 *   <te latency="42" ul_bw="500" dl_bw="800" relay_name="mxp1c01"/>
 *   <te xlatency="55" relay_name="mxp1c02"/>
 * </relaylatency>
 * }
 *
 * @see RelayLatencyEntry
 * @see SignalingType#RELAY_LATENCY
 */
public final class RelayLatencyStanza implements CallMessage {
    /**
     * The wire element tag for a relay latency message.
     */
    public static final String ELEMENT = "relaylatency";

    /**
     * The sentinel value standing in for an absent {@code transaction-id} attribute.
     */
    private static final int UNSET = -1;

    /**
     * The wire attribute marking a call that involves a bot.
     */
    private static final String HAS_BOT_ATTRIBUTE = "has-bot";

    /**
     * The wire attribute naming the probe transaction identifier.
     */
    private static final String TRANSACTION_ID_ATTRIBUTE = "transaction-id";

    /**
     * The wire literal a boolean attribute carries when set; booleans on the call plane serialize as
     * {@code '1'}/{@code '0'} rather than {@code true}/{@code false}.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The call identifier this message's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this message's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * Whether the {@code has-bot} attribute marks a call that involves a bot.
     */
    private final boolean hasBot;

    /**
     * The {@code transaction-id} attribute, or {@code -1} when absent.
     */
    private final int transactionId;

    /**
     * The {@code <te>} latency entries, in wire order; never {@code null}.
     */
    private final List<RelayLatencyEntry> entries;

    /**
     * Constructs a relay latency message, copying the entry list into an immutable list.
     *
     * @param callId        the call identifier; never {@code null}
     * @param callCreator   the call creator's device JID; never {@code null}
     * @param hasBot        whether the {@code has-bot} attribute marks a call that involves a bot
     * @param transactionId the {@code transaction-id} attribute, or {@code -1} when absent
     * @param entries       the {@code <te>} latency entries, in wire order; never {@code null}
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code entries} is
     *                              {@code null}
     */
    public RelayLatencyStanza(String callId,
                              Jid callCreator,
                              boolean hasBot,
                              int transactionId,
                              List<RelayLatencyEntry> entries) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        this.hasBot = hasBot;
        this.transactionId = transactionId;
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries cannot be null"));
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a relay latency message
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a relay latency message
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns whether the {@code has-bot} attribute marks a call that involves a bot.
     *
     * @return {@code true} when this message marks a call that involves a bot
     */
    public boolean hasBot() {
        return hasBot;
    }

    /**
     * Returns the {@code transaction-id} attribute, or {@code -1} when absent.
     *
     * @return the transaction id, or {@code -1} when absent
     */
    public int transactionId() {
        return transactionId;
    }

    /**
     * Returns the {@code <te>} latency entries, in wire order.
     *
     * @return the immutable latency entry list; never {@code null}
     */
    public List<RelayLatencyEntry> entries() {
        return entries;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#RELAY_LATENCY}
     */
    @Override
    public SignalingType type() {
        return SignalingType.RELAY_LATENCY;
    }

    /**
     * Returns the probe transaction identifier, if present.
     *
     * @return an {@link OptionalInt} holding the {@code transaction-id}, or empty when absent
     */
    public OptionalInt transactionIdValue() {
        return transactionId == UNSET ? OptionalInt.empty() : OptionalInt.of(transactionId);
    }

    /**
     * Builds the {@code <relaylatency>} action stanza for this message.
     *
     * <p>The stanza stamps {@code call-id} and {@code call-creator} as every action does; {@code has-bot}
     * is written only when set and {@code transaction-id} is omitted when absent. Each latency entry
     * becomes a {@code <te>} child.
     *
     * @return the relay latency action stanza
     */
    @Override
    public Stanza toStanza() {
        var children = new ArrayList<Stanza>(entries.size());
        for (var entry : entries) {
            children.add(entry.toNode());
        }
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(HAS_BOT_ATTRIBUTE, FLAG_TRUE, hasBot)
                .attribute(TRANSACTION_ID_ATTRIBUTE, transactionId, transactionId != UNSET);
        if (!children.isEmpty()) {
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <relaylatency>} action stanza into a {@link RelayLatencyStanza}.
     *
     * <p>The {@code <te>} children are decoded through {@link RelayLatencyEntry#of(Stanza)}; a {@code <te>}
     * that does not decode is skipped.
     *
     * @param stanza the {@code <relaylatency>} stanza
     * @return the decoded message
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static RelayLatencyStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var hasBot = FLAG_TRUE.equals(stanza.getAttributeAsString(HAS_BOT_ATTRIBUTE, null));
        var transactionId = stanza.getAttributeAsInt(TRANSACTION_ID_ATTRIBUTE, UNSET);
        var entries = new ArrayList<RelayLatencyEntry>();
        for (var child : stanza.getChildren(RelayLatencyEntry.ELEMENT)) {
            RelayLatencyEntry.of(child).ifPresent(entries::add);
        }
        return new RelayLatencyStanza(callId, callCreator, hasBot, transactionId, entries);
    }

    /**
     * Returns whether {@code obj} is a {@link RelayLatencyStanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a value equal relay latency message
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RelayLatencyStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && hasBot == that.hasBot
                && transactionId == that.transactionId
                && entries.equals(that.entries));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this relay latency message
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, hasBot, transactionId, entries);
    }

    /**
     * Returns a diagnostic string for this relay latency message.
     *
     * @return the diagnostic string
     */
    @Override
    public String toString() {
        return "RelayLatencyStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", hasBot=" + hasBot
                + ", transactionId=" + transactionId
                + ", entries=" + entries + ']';
    }
}
