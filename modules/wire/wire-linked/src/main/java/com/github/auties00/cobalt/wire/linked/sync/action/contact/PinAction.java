package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A sync action that records a chat being pinned or unpinned in the chat
 * list.
 *
 * <p>Pinned chats are shown at the top of the chat list regardless of when
 * their most recent message was received. This action is emitted whenever a
 * chat is pinned or unpinned so that every linked device preserves the same
 * ordering and the same set of pinned chats.
 *
 * <p>Each entry is indexed by the chat JID through {@link PinActionArgs} and
 * is replicated via the {@link SyncPatchType#REGULAR_LOW} collection.
 */
@ProtobufMessage(name = "SyncActionValue.PinAction")
public final class PinAction implements SyncAction<PinActionArgs> {
    /**
     * The canonical action name {@code "pin_v1"} used to identify this
     * action inside a sync patch.
     */
    public static final String ACTION_NAME = "pin_v1";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 5;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name {@code "pin_v1"}.
     *
     * @return the string {@code "pin_v1"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version declared by this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Whether the referenced chat is currently pinned to the top of the chat
     * list.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean pinned;


    /**
     * Constructs a new {@code PinAction}. Intended to be invoked by the
     * generated builder and by the protobuf deserializer.
     *
     * @param pinned whether the chat is pinned, or {@code null} to leave the
     *               flag unset
     */
    PinAction(Boolean pinned) {
        this.pinned = pinned;
    }

    /**
     * Returns whether the chat is currently pinned, coalescing an absent
     * value to {@code false}.
     *
     * @return {@code true} if the chat is pinned, otherwise {@code false}
     */
    public boolean pinned() {
        return pinned != null && pinned;
    }

    /**
     * Updates whether the chat is currently pinned.
     *
     * @param pinned the new flag value, or {@code null} to clear the field
     */
    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }


}
