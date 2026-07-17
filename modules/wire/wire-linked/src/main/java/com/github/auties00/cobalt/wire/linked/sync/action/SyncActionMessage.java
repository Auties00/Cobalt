package com.github.auties00.cobalt.wire.linked.sync.action;

import com.github.auties00.cobalt.wire.core.message.MessageKey;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Reference to a single message inside an app state sync action, combining
 * the {@link MessageKey} that uniquely identifies the message with the
 * message timestamp.
 *
 * <p>Used by actions that need to pin a mutation to an exact historical
 * message (for example, starring a message or deleting a message for the
 * current user) so that the same mutation applied on another device
 * resolves to the same target message.
 */
@ProtobufMessage(name = "SyncActionValue.SyncActionMessage")
public final class SyncActionMessage implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name identifying this action category on the wire.
     */
    public static final String ACTION_NAME = "sync_action_message";

    /**
     * Canonical action schema version for this action category.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name used as the first element of the
     * encoded index array.
     *
     * @return the canonical action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version declared by this action.
     *
     * @return the action schema version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Key identifying the referenced message, including its remote JID,
     * from-me flag, and message id.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * Wall clock time associated with the referenced message, encoded on
     * the wire in whole seconds.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * Constructs a new sync action message reference.
     *
     * @param key the key identifying the referenced message
     * @param timestamp the wall clock time of the referenced message
     */
    SyncActionMessage(MessageKey key, Instant timestamp) {
        this.key = key;
        this.timestamp = timestamp;
    }

    /**
     * Returns the key identifying the referenced message.
     *
     * @return an optional containing the message key, empty if absent
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the wall clock time associated with the referenced message.
     *
     * @return an optional containing the timestamp, empty if absent
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the key identifying the referenced message.
     *
     * @param key the message key
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the wall clock time associated with the referenced message.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
