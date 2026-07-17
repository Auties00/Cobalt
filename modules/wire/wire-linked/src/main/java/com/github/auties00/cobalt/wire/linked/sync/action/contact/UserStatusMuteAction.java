package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A sync action that records whether a contact's status updates are muted
 * for the current user.
 *
 * <p>Status updates are the 24-hour disappearing posts contacts can publish
 * on their "Status" feed. Muting a contact's status hides their posts from
 * the feed without affecting chat messages. This action is emitted whenever
 * a contact is muted or unmuted on the status feed so that every linked
 * device shows the same visibility preferences.
 *
 * <p>Each entry is indexed by the contact's JID through
 * {@link UserStatusMuteActionArgs} and is replicated via the
 * {@link SyncPatchType#REGULAR_HIGH} collection.
 */
@ProtobufMessage(name = "SyncActionValue.UserStatusMuteAction")
public final class UserStatusMuteAction implements SyncAction<UserStatusMuteActionArgs> {
    /**
     * The canonical action name {@code "userStatusMute"} used to identify
     * this action inside a sync patch.
     */
    public static final String ACTION_NAME = "userStatusMute";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name {@code "userStatusMute"}.
     *
     * @return the string {@code "userStatusMute"}
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
     * Whether the contact's status feed is muted for the current user.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean muted;


    /**
     * Constructs a new {@code UserStatusMuteAction}. Intended to be invoked
     * by the generated builder and by the protobuf deserializer.
     *
     * @param muted whether the contact's status is muted, or {@code null} to
     *              leave the flag unset
     */
    UserStatusMuteAction(Boolean muted) {
        this.muted = muted;
    }

    /**
     * Returns whether the contact's status feed is muted, coalescing an
     * absent value to {@code false}.
     *
     * <p>Callers that must distinguish an explicitly missing field from an
     * explicit {@code false} should use {@link #rawMuted()} instead.
     *
     * @return {@code true} if the contact's status is muted, otherwise
     *         {@code false}
     */
    public boolean muted() {
        return muted != null && muted;
    }

    /**
     * Returns the raw nullable flag as provided by the remote sync action,
     * preserving the distinction between an absent field and an explicitly
     * set value.
     *
     * @return an {@link Optional} containing the raw {@link Boolean} value,
     *         or an empty {@code Optional} if the field was not present on
     *         the wire
     */
    public Optional<Boolean> rawMuted() {
        return Optional.ofNullable(muted);
    }

    /**
     * Updates whether the contact's status feed is muted for the current
     * user.
     *
     * @param muted the new flag value, or {@code null} to clear the field
     */
    public void setMuted(Boolean muted) {
        this.muted = muted;
    }


}
