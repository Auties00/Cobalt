package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.calls.signaling.relay.RelayInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@link AckResult} variant returned for {@code <ack class="call">} stanzas.
 *
 * <p>Call ACKs confirm that the server processed an outbound call-signaling stanza. On a
 * {@code type="offer"} ACK the body always contains one {@code <relay>} child; on success it is
 * fully populated with the {@link RelayInfo#tokens() tokens}, {@link RelayInfo#authTokens() auth
 * tokens}, {@link RelayInfo#endpoints() endpoints}, {@link RelayInfo#keyValue() call key},
 * and {@link RelayInfo#hbhKeyValue() hop-by-hop key} the call layer drives the media-plane handshake
 * against, and on a NACK it carries only the denormalised {@code call-creator} / {@code call-id}
 * attributes. Other call-class ACK types ({@code accept}, {@code reject}, etc.) typically carry
 * no relay block.
 *
 * <p>The relay block is parsed into the calls {@link RelayInfo} model. That model carries the relay
 * authentication credentials as the list of {@code <auth_token id>} children through
 * {@link RelayInfo#authTokens()}, each referenced by a {@code <te2>} endpoint's {@code auth_token_id},
 * and it exposes the raw call-key and hop-by-hop key bytes through {@link RelayInfo#keyValue()} and
 * {@link RelayInfo#hbhKeyValue()}.
 */
public final class CallAck implements AckResult {
    private final String id;
    private final Instant timestamp;
    private final String type;
    private final Jid from;
    private final Jid participant;
    private final Jid recipient;
    private final Integer error;
    private final RelayInfo relay;

    /**
     * Constructs a call ack snapshot. Package-private; the only caller is {@link AckParser}.
     */
    CallAck(String id, Instant timestamp, String type, Jid from, Jid participant, Jid recipient,
            Integer error, RelayInfo relay) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.from = from;
        this.participant = participant;
        this.recipient = recipient;
        this.error = error;
        this.relay = relay;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public AckClass ackClass() {
        return AckClass.CALL;
    }

    @Override
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    @Override
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    @Override
    public Optional<Jid> from() {
        return Optional.ofNullable(from);
    }

    @Override
    public Optional<Jid> participant() {
        return Optional.ofNullable(participant);
    }

    @Override
    public Optional<Jid> recipient() {
        return Optional.ofNullable(recipient);
    }

    @Override
    public OptionalInt error() {
        return error != null ? OptionalInt.of(error) : OptionalInt.empty();
    }

    /**
     * Returns the parsed {@code <relay>} child of the {@code <ack>} stanza.
     *
     * @return the relay block, or {@link Optional#empty()} when none was present
     */
    public Optional<RelayInfo> relay() {
        return Optional.ofNullable(relay);
    }

    @Override
    public String toString() {
        return "CallAck[id=" + id + ", type=" + type + ", error=" + error
                + ", relay=" + (relay != null) + ']';
    }
}
