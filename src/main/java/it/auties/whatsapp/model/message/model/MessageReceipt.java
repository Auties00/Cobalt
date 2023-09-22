package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * A model that represents the receipt for a message
 */
public final class MessageReceipt implements ProtobufMessage {
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    @Nullable
    private Long deliveredTimestampSeconds;
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    @Nullable
    private Long readTimestampSeconds;
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    @Nullable
    private Long playedTimestampSeconds;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING, repeated = true)
    @NonNull
    private final Set<ContactJid> deliveredJids;
    @ProtobufProperty(index = 6, type = ProtobufType.STRING, repeated = true)
    @NonNull
    private final Set<ContactJid> readJids;

    public MessageReceipt() {
        this.deliveredJids = new HashSet<>();
        this.readJids = new HashSet<>();
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MessageReceipt(@Nullable Long deliveredTimestampSeconds, @Nullable Long readTimestampSeconds, @Nullable Long playedTimestampSeconds, @NonNull Set<ContactJid> deliveredJids, @NonNull Set<ContactJid> readJids) {
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

    public Set<ContactJid> deliveredJids() {
        return deliveredJids;
    }

    public Set<ContactJid> readJids() {
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