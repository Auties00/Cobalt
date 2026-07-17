package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A sync action that records a message being starred or unstarred.
 *
 * <p>Starring a message places it in the "Starred messages" list that the
 * user can browse later. This action is emitted whenever a message is
 * starred or unstarred so that every linked device shows the same set of
 * favourite messages.
 *
 * <p>Each starred message is identified by the original message key
 * ({@code remote}, {@code id}, {@code fromMe}, {@code participant}) through
 * {@link StarActionArgs} and is replicated via the
 * {@link SyncPatchType#REGULAR_HIGH} collection.
 */
@ProtobufMessage(name = "SyncActionValue.StarAction")
public final class StarAction implements SyncAction<StarActionArgs> {
    /**
     * The canonical action name {@code "star"} used to identify this action
     * inside a sync patch.
     */
    public static final String ACTION_NAME = "star";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 2;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name {@code "star"}.
     *
     * @return the string {@code "star"}
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
     * Whether the referenced message is currently starred.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean starred;


    /**
     * Constructs a new {@code StarAction}. Intended to be invoked by the
     * generated builder and by the protobuf deserializer.
     *
     * @param starred whether the message is starred, or {@code null} to leave
     *                the flag unset
     */
    StarAction(Boolean starred) {
        this.starred = starred;
    }

    /**
     * Returns whether the message is currently starred, coalescing an absent
     * value to {@code false}.
     *
     * @return {@code true} if the message is starred, otherwise {@code false}
     */
    public boolean starred() {
        return starred != null && starred;
    }

    /**
     * Updates whether the message is currently starred.
     *
     * @param starred the new flag value, or {@code null} to clear the field
     */
    public void setStarred(Boolean starred) {
        this.starred = starred;
    }


}
