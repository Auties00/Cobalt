package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that marks a chat as read or unread on the current account.
 *
 * <p>When the user manually marks a chat as read or unread, the unread
 * counter is adjusted locally and this action is pushed through app state
 * sync so every linked device reflects the same status.
 *
 * <p>The optional {@link SyncActionMessageRange} records the portion of
 * messages that existed when the chat was marked, allowing receivers to
 * tell whether newer messages have arrived that should reset the unread
 * state.
 */
@ProtobufMessage(name = "SyncActionValue.MarkChatAsReadAction")
public final class MarkChatAsReadAction implements SyncAction<MarkChatAsReadActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "markChatAsRead";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "markChatAsRead"}
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
     * Whether the chat has been marked as read ({@code true}) or as unread
     * ({@code false}). {@code null} is treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean read;

    /**
     * Optional message range identifying the portion of the conversation
     * visible when the mark-as-read operation was performed.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SyncActionMessageRange messageRange;


    /**
     * Constructs a new {@code MarkChatAsReadAction} with the given fields.
     *
     * @param read         the new read flag
     * @param messageRange optional message range snapshot
     */
    MarkChatAsReadAction(Boolean read, SyncActionMessageRange messageRange) {
        this.read = read;
        this.messageRange = messageRange;
    }

    /**
     * Returns whether the chat has been marked as read.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false} (unread).
     *
     * @return {@code true} if marked as read, {@code false} otherwise
     */
    public boolean read() {
        return read != null && read;
    }

    /**
     * Returns the message range snapshot associated with this action.
     *
     * @return an {@link Optional} containing the {@link SyncActionMessageRange},
     *         or an empty {@code Optional} when no range was captured
     */
    public Optional<SyncActionMessageRange> messageRange() {
        return Optional.ofNullable(messageRange);
    }

    /**
     * Sets whether the chat is marked as read.
     *
     * @param read the new read flag, or {@code null} to clear it
     */
    public void setRead(Boolean read) {
        this.read = read;
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
