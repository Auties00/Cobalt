package com.github.auties00.cobalt.wire.linked.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Holds the user preferences that control the chat lock feature.
 *
 * <p>The chat lock feature allows a user to hide individual conversations
 * behind a secret code (in addition to the device level lock). When enabled,
 * locked chats are moved to a dedicated folder that is only reachable after
 * the user enters the secret code. This settings object both exposes the
 * preferences to the rest of the client and acts as a {@link SyncAction} so
 * that changes to the preferences are propagated to the other linked devices
 * through the app state sync protocol.
 *
 * @see UserPassword
 * @see SyncAction
 */
@ProtobufMessage(name = "ChatLockSettings")
public final class ChatLockSettings implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action identifier used when this settings object is serialised
     * as a sync action during app state synchronisation.
     */
    public static final String ACTION_NAME = "setting_chatLock";

    /**
     * Canonical action version reported during app state synchronisation.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Sync patch collection that carries this settings object when it is
     * synchronised between linked devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action identifier for this sync action.
     *
     * @return the value of {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version for this sync action.
     *
     * @return the value of {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Whether locked chats are hidden from the main chat list.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean hideLockedChats;

    /**
     * The secret code required to unlock the locked chats folder.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    UserPassword secretCode;


    /**
     * Constructs a new chat lock settings instance with the given values.
     *
     * @param hideLockedChats whether locked chats should be hidden, may be {@code null}
     * @param secretCode      the secret code that unlocks the locked chats folder, may be {@code null}
     */
    ChatLockSettings(Boolean hideLockedChats, UserPassword secretCode) {
        this.hideLockedChats = hideLockedChats;
        this.secretCode = secretCode;
    }

    /**
     * Returns whether locked chats are hidden from the main chat list.
     *
     * <p>A missing value is interpreted as {@code false}, meaning that locked
     * chats are still visible but require the secret code to be opened.
     *
     * @return {@code true} if locked chats are hidden, {@code false} otherwise
     */
    public boolean hideLockedChats() {
        return hideLockedChats != null && hideLockedChats;
    }

    /**
     * Returns the secret code that unlocks the locked chats folder.
     *
     * @return an {@link Optional} containing the secret code, or empty if not set
     */
    public Optional<UserPassword> secretCode() {
        return Optional.ofNullable(secretCode);
    }

    /**
     * Updates the flag that hides locked chats from the main chat list.
     *
     * @param hideLockedChats the new value, or {@code null} to unset the field
     */
    public void setHideLockedChats(Boolean hideLockedChats) {
        this.hideLockedChats = hideLockedChats;
    }

    /**
     * Updates the secret code that unlocks the locked chats folder.
     *
     * @param secretCode the new secret code, or {@code null} to unset the field
     */
    public void setSecretCode(UserPassword secretCode) {
        this.secretCode = secretCode;
    }
}
