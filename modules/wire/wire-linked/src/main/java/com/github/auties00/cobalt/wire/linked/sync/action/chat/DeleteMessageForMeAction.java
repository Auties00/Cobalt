package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.time.Instant;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that deletes a single message from the current user's view of
 * a chat (delete-for-me).
 *
 * <p>This is the local counterpart to the global delete-for-everyone
 * revocation. The message is only removed from the user's own linked
 * devices; other participants in the chat keep their copy. The action
 * identifies the message by the full message key carried in its companion
 * {@link DeleteMessageForMeActionArgs}.
 */
@ProtobufMessage(name = "SyncActionValue.DeleteMessageForMeAction")
public final class DeleteMessageForMeAction implements SyncAction<DeleteMessageForMeActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "deleteMessageForMe";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "deleteMessageForMe"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 3}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Whether attached media for the deleted message should also be removed
     * from local storage. {@code null} is treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean deleteMedia;

    /**
     * Timestamp of the message being deleted, stored as an {@link Instant}
     * serialised in seconds via {@link InstantSecondsMixin}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant messageTimestamp;


    /**
     * Constructs a new {@code DeleteMessageForMeAction} with the given
     * fields.
     *
     * @param deleteMedia      whether the message's media should also be deleted
     * @param messageTimestamp the timestamp of the message being deleted
     */
    DeleteMessageForMeAction(Boolean deleteMedia, Instant messageTimestamp) {
        this.deleteMedia = deleteMedia;
        this.messageTimestamp = messageTimestamp;
    }

    /**
     * Returns whether the message's media should also be deleted.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false}.
     *
     * @return {@code true} when attached media should be removed
     */
    public boolean deleteMedia() {
        return deleteMedia != null && deleteMedia;
    }

    /**
     * Returns the original timestamp of the message being deleted.
     *
     * @return an {@link Optional} containing the message timestamp, or an
     *         empty {@code Optional} when no timestamp was recorded
     */
    public Optional<Instant> messageTimestamp() {
        return Optional.ofNullable(messageTimestamp);
    }

    /**
     * Sets whether the message's media should also be deleted.
     *
     * @param deleteMedia the new delete-media flag, or {@code null} to clear it
     */
    public void setDeleteMedia(Boolean deleteMedia) {
        this.deleteMedia = deleteMedia;
    }

    /**
     * Sets the message timestamp.
     *
     * @param messageTimestamp the new timestamp, or {@code null} to clear it
     */
    public void setMessageTimestamp(Instant messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }


}
