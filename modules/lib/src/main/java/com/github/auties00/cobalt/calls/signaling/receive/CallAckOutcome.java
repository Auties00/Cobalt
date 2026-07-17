package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.CallAck;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.relay.RelayInfo;

/**
 * Decoded result of the synchronous {@code <ack class="call">} the server returns for an outbound
 * offer or accept.
 *
 * <p>The relay credentials a call needs to bring up its media plane are not pushed asynchronously;
 * they are the synchronous reply to the offer. An outbound offer is shipped through the
 * identifier correlated {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)}, which blocks the
 * calling virtual thread until the matching {@code <ack class="call" type="offer">} arrives and
 * returns it as the call value. On success that ack carries one {@code <relay>} child fully
 * populated with the {@link RelayInfo#tokens() relay tokens}, {@link RelayInfo#endpoints() edge
 * endpoints}, {@link RelayInfo#keyValue() call key}, and {@link RelayInfo#hbhKeyValue() hop by hop
 * key}; on a NACK it carries an {@code error} code and a {@code <relay>} that holds only the
 * denormalised {@code call-creator} and {@code call-id} attributes. The outbound accept follows the
 * same contract, so the same outcome models both legs.
 *
 * <p>This type is the typed projection of that return stanza, shaped like {@link CallAck} so callers
 * move between the two without surprise: {@link #relay()} is the parsed {@link RelayInfo} when the ack
 * carried a usable {@code <relay>} child; {@link #error()} carries the server's NACK code;
 * {@link #isAck()} is the boolean accept or reject test the caller branches on before reading the
 * relay. An outcome with {@link #isNack()} is the engine's signal to abandon the call setup with the
 * corresponding result.
 *
 * @see CallAckParser
 * @see RelayInfo
 */
public final class CallAckOutcome {
    /**
     * The {@code id} attribute echoed from the outbound stanza, correlating the ack to its request.
     */
    private final String id;

    /**
     * The {@code type} attribute naming the acked leg ({@code "offer"} or {@code "accept"}), or
     * {@code null} when the server omitted it.
     */
    private final String type;

    /**
     * The {@code from} attribute identifying the originator of the ack, or {@code null} when absent.
     */
    private final Jid from;

    /**
     * The server NACK code, or {@code null} when the send succeeded.
     */
    private final Integer error;

    /**
     * The parsed {@code <relay>} block, or {@code null} when the ack carried no usable relay child.
     */
    private final RelayInfo relay;

    /**
     * Constructs an outcome from its already decoded fields.
     *
     * <p>Consumers obtain an instance from {@link CallAckParser#parse(Stanza)} rather than calling this
     * constructor directly, keeping {@link CallAckParser} the single construction point.
     *
     * @param id    the correlation identifier echoed from the outbound stanza
     * @param type  the leg type, or {@code null} when absent
     * @param from  the originator JID, or {@code null} when absent
     * @param error the server NACK code, or {@code null} when the send succeeded
     * @param relay the parsed relay block, or {@code null} when none was present
     * @throws NullPointerException if {@code id} is {@code null}
     */
    CallAckOutcome(String id, String type, Jid from, Integer error, RelayInfo relay) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = type;
        this.from = from;
        this.error = error;
        this.relay = relay;
    }

    /**
     * Adapts a reused {@link CallAck} envelope and a calls native {@link RelayInfo} relay block into
     * an outcome.
     *
     * <p>The {@link AckClass#CALL} envelope ({@code id}, {@code type}, {@code from}, {@code error}) is
     * taken verbatim from the shared {@link CallAck}, while the relay block is the calls model the
     * media plane consumes rather than the legacy relay carried on {@link CallAck#relay()}. Consumed by
     * {@link CallAckParser}.
     *
     * @param ack   the reused {@link CallAck} the shared ack parser produced for the return stanza
     * @param relay the calls {@link RelayInfo} re parsed from the ack's {@code <relay>} child, or
     *              {@code null} when none was present
     * @return the typed outcome
     * @throws NullPointerException if {@code ack} is {@code null} or carries no {@code id}
     */
    static CallAckOutcome of(CallAck ack, RelayInfo relay) {
        Objects.requireNonNull(ack, "ack cannot be null");
        return new CallAckOutcome(
                ack.id(),
                ack.type().orElse(null),
                ack.from().orElse(null),
                ack.error().isPresent() ? ack.error().getAsInt() : null,
                relay);
    }

    /**
     * Returns the {@code id} attribute echoed from the outbound stanza.
     *
     * @return the correlation identifier; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the {@code type} attribute naming the acked leg.
     *
     * @return the leg type ({@code "offer"} or {@code "accept"}), or {@link Optional#empty()} when the
     *         server omitted it
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the {@code from} attribute identifying the originator of the ack.
     *
     * @return the originator JID, or {@link Optional#empty()} when absent
     */
    public Optional<Jid> from() {
        return Optional.ofNullable(from);
    }

    /**
     * Returns the server NACK code carried on the ack.
     *
     * <p>The presence of a value is the canonical accept or reject signal; {@link #isAck()} is the
     * boolean shortcut for callers that only need the accept or reject test, while the integer drives
     * the result and end reason mapping on the NACK path.
     *
     * @return the {@code error} code, or {@link OptionalInt#empty()} when the send succeeded
     */
    public OptionalInt error() {
        return error != null ? OptionalInt.of(error) : OptionalInt.empty();
    }

    /**
     * Returns the parsed {@code <relay>} block.
     *
     * <p>Populated with the full token, endpoint, and key set on a successful offer ack, and with only
     * the denormalised {@code call-creator} and {@code call-id} attributes on a NACK; absent when the
     * ack carried no {@code <relay>} child at all.
     *
     * @return the {@link RelayInfo}, or {@link Optional#empty()} when no relay child was present
     */
    public Optional<RelayInfo> relay() {
        return Optional.ofNullable(relay);
    }

    /**
     * Returns whether the server accepted the outbound offer or accept.
     *
     * <p>Equivalent to {@code error().isEmpty()}; an accepting ack on the offer leg is the one that
     * carries the usable {@link #relay() relay} for media bring up.
     *
     * @return {@code true} when no {@code error} attribute was present
     */
    public boolean isAck() {
        return error == null;
    }

    /**
     * Returns whether the server rejected the outbound offer or accept.
     *
     * <p>Equivalent to {@code error().isPresent()}; a NACK is the engine's signal to abandon call setup
     * with the result mapped from {@link #error()}.
     *
     * @return {@code true} when an {@code error} attribute was present
     */
    public boolean isNack() {
        return error != null;
    }

    /**
     * Returns a debug rendering naming the leg, the accept or reject decision, and whether a relay block
     * was present.
     *
     * @return a string for diagnostics
     */
    @Override
    public String toString() {
        return "CallAckOutcome[id=" + id + ", type=" + type + ", error=" + error
                + ", relay=" + (relay != null) + ']';
    }
}
