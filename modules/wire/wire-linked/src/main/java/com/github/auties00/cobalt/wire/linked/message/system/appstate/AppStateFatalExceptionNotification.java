package com.github.auties00.cobalt.wire.linked.message.system.appstate;

import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Peer message emitted when a device encounters an unrecoverable error
 * while processing the multi-device app state stream.
 *
 * <p>When a companion cannot reconcile one or more app state collections
 * (chats, contacts, settings, starred messages, labels, and similar
 * synchronised collections) because of a fatal decryption or validation
 * failure, it notifies the primary device by sending this message. The
 * primary device can then trigger a full resynchronisation of the
 * affected collections on the companion.
 *
 * <p>The payload identifies which collections failed and when the
 * failure was observed, so the primary can decide how to recover.
 */
@ProtobufMessage(name = "Message.AppStateFatalExceptionNotification")
public final class AppStateFatalExceptionNotification implements Message {
    /**
     * The names of the app state collections for which an unrecoverable
     * failure was observed.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    List<String> collectionNames;

    /**
     * The instant at which the failure was observed, expressed in
     * milliseconds since the epoch on the wire.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant timestamp;


    /**
     * Creates a new notification describing an unrecoverable app state
     * failure.
     *
     * @param collectionNames the names of the affected collections, or
     *                        {@code null} if unset
     * @param timestamp       the instant at which the failure occurred,
     *                        or {@code null} if unset
     */
    AppStateFatalExceptionNotification(List<String> collectionNames, Instant timestamp) {
        this.collectionNames = collectionNames;
        this.timestamp = timestamp;
    }

    /**
     * Returns the names of the app state collections that experienced an
     * unrecoverable failure.
     *
     * @return an unmodifiable {@link List} of collection names, or an
     *         empty list if none has been set
     */
    public List<String> collectionNames() {
        return collectionNames == null ? List.of() : Collections.unmodifiableList(collectionNames);
    }

    /**
     * Returns the instant at which the failure was observed, if present.
     *
     * @return an {@link Optional} containing the {@link Instant}, or
     *         {@link Optional#empty()} if no timestamp has been set
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the names of the app state collections that experienced an
     * unrecoverable failure.
     *
     * @param collectionNames the collection names to assign, or
     *                        {@code null} to clear
     */
    public void setCollectionNames(List<String> collectionNames) {
        this.collectionNames = collectionNames;
    }

    /**
     * Sets the instant at which the failure was observed.
     *
     * @param timestamp the {@link Instant} to assign, or {@code null} to
     *                  clear
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
