package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * A model that represents the receipt for a message
 */
@ProtobufMessage(name = "UserReceipt")
public final class MessageReceipt {
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    private Long deliveredTimestampSeconds;
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    private Long readTimestampSeconds;
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    private Long playedTimestampSeconds;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final Set<Jid> deliveredJids;
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final Set<Jid> readJids;

    public MessageReceipt() {
        this.deliveredJids = new HashSet<>();
        this.readJids = new HashSet<>();
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MessageReceipt(Long deliveredTimestampSeconds, Long readTimestampSeconds, Long playedTimestampSeconds, Set<Jid> deliveredJids, Set<Jid> readJids) {
        this.deliveredTimestampSeconds = deliveredTimestampSeconds;
        this.readTimestampSeconds = readTimestampSeconds;
        this.playedTimestampSeconds = playedTimestampSeconds;
        this.deliveredJids = deliveredJids;
        this.readJids = readJids;
    }

    public OptionalLong deliveredTimestampSeconds() {
        return Clock.parseTimestamp(deliveredTimestampSeconds);
    }

    /**
     * Returns the date when the message was delivered
     *
     * @return a non-null optional
     */
    public Optional<ZonedDateTime> deliveredTimestamp() {
        return Clock.parseSeconds(deliveredTimestampSeconds);
    }

    public OptionalLong readTimestampSeconds() {
        return Clock.parseTimestamp(readTimestampSeconds);
    }

    /**
     * Returns the date when the message was delivered
     *
     * @return a non-null optional
     */
    public Optional<ZonedDateTime> readTimestamp() {
        return Clock.parseSeconds(readTimestampSeconds);
    }

    public OptionalLong playedTimestampSeconds() {
        return Clock.parseTimestamp(playedTimestampSeconds);
    }

    /**
     * Returns the date when the message was played
     *
     * @return a non-null optional
     */
    public Optional<ZonedDateTime> playedTimestamp() {
        return Clock.parseSeconds(playedTimestampSeconds);
    }

    public Set<Jid> deliveredJids() {
        return deliveredJids;
    }

    public Set<Jid> readJids() {
        return readJids;
    }

    /**
     * Sets the read timestampSeconds
     *
     * @param readTimestampSeconds the timestampSeconds
     * @return the same instance
     */
    public MessageReceipt readTimestampSeconds(long readTimestampSeconds) {
        if (deliveredTimestampSeconds == null) {
            this.deliveredTimestampSeconds = readTimestampSeconds;
        }
        this.readTimestampSeconds = readTimestampSeconds;
        return this;
    }

    /**
     * Sets the played timestampSeconds
     *
     * @param playedTimestampSeconds the timestampSeconds
     * @return the same instance
     */
    public MessageReceipt playedTimestampSeconds(long playedTimestampSeconds) {
        if (deliveredTimestampSeconds == null) {
            this.deliveredTimestampSeconds = playedTimestampSeconds;
        }
        if (readTimestampSeconds == null) {
            this.readTimestampSeconds = playedTimestampSeconds;
        }
        this.playedTimestampSeconds = playedTimestampSeconds;
        return this;
    }
}