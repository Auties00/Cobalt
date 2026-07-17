package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Sync action that locks or unlocks a chat on the current account.
 *
 * <p>A locked chat is hidden behind the device's biometric or passcode
 * prompt so its content and notifications remain private. This action
 * replicates the locked state of a chat across every linked device via the
 * app state sync protocol.
 */
@ProtobufMessage(name = "SyncActionValue.LockChatAction")
public final class LockChatAction implements SyncAction<LockChatActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "lock";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "lock"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 7}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Whether the chat is currently locked ({@code true}) or unlocked
     * ({@code false}). {@code null} is treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean locked;


    /**
     * Constructs a new {@code LockChatAction} with the given locked flag.
     *
     * @param locked the new locked flag
     */
    LockChatAction(Boolean locked) {
        this.locked = locked;
    }

    /**
     * Returns whether the chat is locked.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false}.
     *
     * @return {@code true} if the chat is locked, {@code false} otherwise
     */
    public boolean locked() {
        return locked != null && locked;
    }

    /**
     * Sets whether the chat is locked.
     *
     * @param locked the new locked flag, or {@code null} to clear it
     */
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }


}
