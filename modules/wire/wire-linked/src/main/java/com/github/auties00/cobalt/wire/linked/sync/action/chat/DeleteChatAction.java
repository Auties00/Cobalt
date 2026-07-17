package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that deletes a chat from the conversation list on the current
 * account.
 *
 * <p>Unlike {@link ClearChatAction}, which only erases the messages, a
 * delete-chat operation removes the entire conversation entry so it no
 * longer appears in the chat list. The action is propagated through app
 * state sync so every linked device applies the same removal.
 *
 * <p>The optional {@link SyncActionMessageRange} records the visible message
 * range at the time of deletion, which receivers can use to reason about
 * newer messages that may have arrived afterwards.
 */
@ProtobufMessage(name = "SyncActionValue.DeleteChatAction")
public final class DeleteChatAction implements SyncAction<DeleteChatActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "deleteChat";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 6;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "deleteChat"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 6}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Optional message range identifying the portion of the conversation
     * visible when the delete operation was performed.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SyncActionMessageRange messageRange;


    /**
     * Constructs a new {@code DeleteChatAction} with the given message range.
     *
     * @param messageRange optional message range snapshot
     */
    DeleteChatAction(SyncActionMessageRange messageRange) {
        this.messageRange = messageRange;
    }

    /**
     * Returns the message range snapshot captured when the chat was deleted.
     *
     * @return an {@link Optional} containing the {@link SyncActionMessageRange},
     *         or an empty {@code Optional} when no range was captured
     */
    public Optional<SyncActionMessageRange> messageRange() {
        return Optional.ofNullable(messageRange);
    }

    /**
     * Sets the message range snapshot.
     *
     * @param messageRange the new message range, or {@code null} to clear it
     */
    public void setMessageRange(SyncActionMessageRange messageRange) {
        this.messageRange = messageRange;
    }


}
