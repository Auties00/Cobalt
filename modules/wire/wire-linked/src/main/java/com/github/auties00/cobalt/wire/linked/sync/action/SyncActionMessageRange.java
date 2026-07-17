package com.github.auties00.cobalt.wire.linked.sync.action;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Describes a chat level range of messages attached to an app state sync
 * action, typically used by actions that apply to a chat as a whole but
 * need to bracket the affected messages by time (for example, clearing or
 * deleting the messages of a chat up to a given point).
 *
 * <p>The range carries the last regular message timestamp, the last system
 * message timestamp, and the specific messages enumerated within the range,
 * so that all linked devices can reconstruct the same bracket without
 * ambiguity.
 */
@ProtobufMessage(name = "SyncActionValue.SyncActionMessageRange")
public final class SyncActionMessageRange implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name identifying this action category on the wire.
     */
    public static final String ACTION_NAME = "sync_action_message_range";

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
     * Wall clock time of the most recent regular (user) message in the
     * range, encoded on the wire in whole seconds.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastMessageTimestamp;

    /**
     * Wall clock time of the most recent system message in the range,
     * encoded on the wire in whole seconds.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastSystemMessageTimestamp;

    /**
     * Individual message references included in the range, enumerated so
     * that all linked devices resolve to the same target messages.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<SyncActionMessage> messages;

    /**
     * Constructs a new sync action message range.
     *
     * @param lastMessageTimestamp the timestamp of the most recent regular
     *                             message in the range
     * @param lastSystemMessageTimestamp the timestamp of the most recent
     *                                   system message in the range
     * @param messages the individual message references included in the
     *                 range
     */
    SyncActionMessageRange(Instant lastMessageTimestamp, Instant lastSystemMessageTimestamp, List<SyncActionMessage> messages) {
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastSystemMessageTimestamp = lastSystemMessageTimestamp;
        this.messages = messages;
    }

    /**
     * Returns the timestamp of the most recent regular message in the
     * range.
     *
     * @return an optional containing the timestamp, empty if absent
     */
    public Optional<Instant> lastMessageTimestamp() {
        return Optional.ofNullable(lastMessageTimestamp);
    }

    /**
     * Returns the timestamp of the most recent system message in the range.
     *
     * @return an optional containing the timestamp, empty if absent
     */
    public Optional<Instant> lastSystemMessageTimestamp() {
        return Optional.ofNullable(lastSystemMessageTimestamp);
    }

    /**
     * Returns an unmodifiable view of the individual message references
     * enumerated in the range.
     *
     * @return the message references, or an empty list if none are set
     */
    public List<SyncActionMessage> messages() {
        return messages == null ? List.of() : Collections.unmodifiableList(messages);
    }

    /**
     * Sets the timestamp of the most recent regular message in the range.
     *
     * @param lastMessageTimestamp the timestamp
     */
    public void setLastMessageTimestamp(Instant lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    /**
     * Sets the timestamp of the most recent system message in the range.
     *
     * @param lastSystemMessageTimestamp the timestamp
     */
    public void setLastSystemMessageTimestamp(Instant lastSystemMessageTimestamp) {
        this.lastSystemMessageTimestamp = lastSystemMessageTimestamp;
    }

    /**
     * Sets the individual message references enumerated in the range.
     *
     * @param messages the message references
     */
    public void setMessages(List<SyncActionMessage> messages) {
        this.messages = messages;
    }
}
