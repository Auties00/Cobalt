package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@link AckResult} variant returned for {@code <ack class="receipt">} stanzas.
 *
 * <p>Receipt ACKs confirm that the server processed an outbound {@code <receipt>} stanza (or its
 * {@code type="retry"} retry-receipt variant). They carry only the common envelope.
 */
public final class ReceiptAck implements AckResult {
    private final String id;
    private final Instant timestamp;
    private final String type;
    private final Jid from;
    private final Jid participant;
    private final Jid recipient;
    private final Integer error;

    /**
     * Constructs a receipt ack snapshot. Package-private; the only caller is {@link AckParser}.
     */
    ReceiptAck(String id, Instant timestamp, String type, Jid from, Jid participant, Jid recipient,
               Integer error) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.from = from;
        this.participant = participant;
        this.recipient = recipient;
        this.error = error;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public AckClass ackClass() {
        return AckClass.RECEIPT;
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

    @Override
    public String toString() {
        return "ReceiptAck[id=" + id + ", error=" + error + ']';
    }
}
