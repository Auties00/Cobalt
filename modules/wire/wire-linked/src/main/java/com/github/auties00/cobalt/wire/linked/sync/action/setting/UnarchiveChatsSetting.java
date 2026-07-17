package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Represents the user's preference for automatically unarchiving chats
 * when they receive new messages, synchronised across linked devices.
 *
 * <p>When enabled, incoming messages in an archived conversation cause
 * the conversation to move back to the main chat list; when disabled,
 * archived conversations remain hidden even as new messages arrive. This
 * preference is account-wide and applies identically on every linked
 * device.
 */
@ProtobufMessage(name = "SyncActionValue.UnarchiveChatsSetting")
public final class UnarchiveChatsSetting implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used when this setting is encoded in an app
     * state sync mutation.
     */
    public static final String ACTION_NAME = "setting_unarchiveChats";

    /**
     * Canonical action version emitted alongside this setting when it is
     * published to the app state sync collection.
     */
    public static final int ACTION_VERSION = 4;

    /**
     * App state collection that stores this setting.
     *
     * <p>Chat management preferences are not latency-sensitive and live
     * in the {@code REGULAR_LOW} patch type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for this setting.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version for this setting.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Whether chats are unarchived automatically when they receive new
     * messages.
     *
     * <p>Encoded as an optional protobuf {@code bool}; a missing value is
     * treated as {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean unarchiveChats;


    /**
     * Constructs a new unarchive-chats setting.
     *
     * @param unarchiveChats the desired value, or {@code null} to leave
     *                       the field unset on the wire
     */
    UnarchiveChatsSetting(Boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
    }

    /**
     * Returns whether the auto-unarchive behaviour is enabled.
     *
     * <p>A missing protobuf field is reported as {@code false}, which
     * matches the default behaviour expected by WhatsApp clients when
     * the setting has never been written.
     *
     * @return {@code true} if archived chats are automatically unarchived
     *         on new messages, {@code false} otherwise
     */
    public boolean unarchiveChats() {
        return unarchiveChats != null && unarchiveChats;
    }

    /**
     * Updates the auto-unarchive preference.
     *
     * @param unarchiveChats the new value, or {@code null} to clear the
     *                       protobuf field
     */
    public void setUnarchiveChats(Boolean unarchiveChats) {
        this.unarchiveChats = unarchiveChats;
    }
}
