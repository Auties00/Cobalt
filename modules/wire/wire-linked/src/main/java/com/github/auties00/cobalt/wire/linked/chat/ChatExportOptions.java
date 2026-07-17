package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Immutable holder for the parameters that tune a chat export.
 *
 * <p>An options instance controls whether media attachments are downloaded and
 * bundled into the archive, an optional inclusive lower bound on the message
 * timestamp, an optional inclusive upper bound on the message timestamp, and a
 * hard cap on the number of messages written.
 */
@ProtobufMessage(name = "ChatExportOptions")
public final class ChatExportOptions {
    /**
     * The message cap applied when the caller does not specify one.
     */
    private static final int DEFAULT_MESSAGE_LIMIT = 100000;

    /**
     * Whether media attachments are downloaded and bundled into the archive.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean includeMedia;

    /**
     * The inclusive lower bound on the message timestamp, or {@code null} when
     * no lower bound is applied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    final Instant startDate;

    /**
     * The inclusive upper bound on the message timestamp, or {@code null} when
     * no upper bound is applied.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    final Instant endDate;

    /**
     * The maximum number of messages to write.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int messageLimit;

    /**
     * Constructs a new options holder with the given configuration.
     *
     * @param includeMedia whether media attachments are downloaded and bundled
     *                     into the archive
     * @param startDate    the inclusive lower bound on the message timestamp,
     *                     or {@code null} for no lower bound
     * @param endDate      the inclusive upper bound on the message timestamp,
     *                     or {@code null} for no upper bound
     * @param messageLimit the maximum number of messages to write
     */
    public ChatExportOptions(boolean includeMedia, Instant startDate, Instant endDate, int messageLimit) {
        this.includeMedia = includeMedia;
        this.startDate = startDate;
        this.endDate = endDate;
        this.messageLimit = messageLimit;
    }

    /**
     * Returns the default options: media excluded, no date bounds, and the
     * standard message cap.
     *
     * @return a non-null options instance with the default configuration
     */
    public static ChatExportOptions defaults() {
        return new ChatExportOptions(false, null, null, DEFAULT_MESSAGE_LIMIT);
    }

    /**
     * Returns whether media attachments are downloaded and bundled into the archive.
     *
     * @return {@code true} if media is included, {@code false} otherwise
     */
    public boolean includeMedia() {
        return includeMedia;
    }

    /**
     * Returns the inclusive lower bound on the message timestamp.
     *
     * @return an {@link Optional} containing the lower bound, or empty if no
     *         lower bound is applied
     */
    public Optional<Instant> startDate() {
        return Optional.ofNullable(startDate);
    }

    /**
     * Returns the inclusive upper bound on the message timestamp.
     *
     * @return an {@link Optional} containing the upper bound, or empty if no
     *         upper bound is applied
     */
    public Optional<Instant> endDate() {
        return Optional.ofNullable(endDate);
    }

    /**
     * Returns the maximum number of messages to write.
     *
     * @return the message cap
     */
    public int messageLimit() {
        return messageLimit;
    }
}