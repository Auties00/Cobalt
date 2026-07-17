package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that clears all messages from a chat on the current account.
 *
 * <p>A clear-chat operation removes messages from the local history of the
 * chat without deleting the conversation itself. The action propagates
 * through app state sync so the user's other devices apply the same cleanup.
 *
 * <p>The companion {@link ClearChatActionArgs} additionally carries flags
 * controlling whether starred messages and media files should also be
 * removed; this message body carries the optional range of messages that
 * were visible at the time of clearing.
 */
@ProtobufMessage(name = "SyncActionValue.ClearChatAction")
public final class ClearChatAction implements SyncAction<ClearChatActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "clearChat";

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
     * @return the string {@code "clearChat"}
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
     * that was visible when the clear operation was performed.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    SyncActionMessageRange messageRange;


    /**
     * Constructs a new {@code ClearChatAction} with the given message range.
     *
     * @param messageRange optional message range snapshot
     */
    ClearChatAction(SyncActionMessageRange messageRange) {
        this.messageRange = messageRange;
    }

    /**
     * Returns the message range snapshot captured when the chat was cleared.
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
