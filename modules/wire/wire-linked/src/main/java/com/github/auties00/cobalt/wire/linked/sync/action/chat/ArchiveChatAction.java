package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that archives or unarchives a chat across the user's linked devices.
 *
 * <p>When a chat is archived it is hidden from the main conversation list until
 * a new message arrives or the user manually unarchives it. This action is
 * replicated through the app state sync protocol so that archiving a chat on
 * one device is reflected on every other linked device.
 *
 * <p>The action carries a boolean flag describing whether the chat is now
 * archived and an optional message range that identifies the portion of the
 * conversation that was visible when the action was performed, allowing
 * receivers to detect whether new messages have arrived in the meantime.
 */
@ProtobufMessage(name = "SyncActionValue.ArchiveChatAction")
public final class ArchiveChatAction implements SyncAction<ArchiveChatActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "archive";

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
     * @return the string {@code "archive"}
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
     * Whether the chat has been archived ({@code true}) or unarchived
     * ({@code false}). {@code null} is treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean archived;

    /**
     * Optional message range that identifies the portion of the conversation
     * visible when the archive action was performed.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SyncActionMessageRange messageRange;


    /**
     * Constructs a new {@code ArchiveChatAction} with the given fields.
     *
     * @param archived     whether the chat is archived
     * @param messageRange optional message range snapshot
     */
    ArchiveChatAction(Boolean archived, SyncActionMessageRange messageRange) {
        this.archived = archived;
        this.messageRange = messageRange;
    }

    /**
     * Returns whether the chat is archived.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false}, treating absence of the flag as an unarchive action.
     *
     * @return {@code true} if the chat is archived, {@code false} otherwise
     */
    public boolean archived() {
        return archived != null && archived;
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
     * Sets whether the chat is archived.
     *
     * @param archived the new archived flag, or {@code null} to clear it
     */
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    /**
     * Sets the message range snapshot associated with this action.
     *
     * @param messageRange the new message range, or {@code null} to clear it
     */
    public void setMessageRange(SyncActionMessageRange messageRange) {
        this.messageRange = messageRange;
    }


}
