package com.github.auties00.cobalt.wire.linked.sync.action.setting;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents the user's push name, synchronised across linked devices.
 *
 * <p>The push name is the display name broadcast to contacts alongside
 * outgoing messages, calls, and presence updates. It is what recipients
 * see for users who are not already saved in their address book. Changing
 * the push name on one device publishes this setting so that every
 * companion device advertises the same value.
 */
@ProtobufMessage(name = "SyncActionValue.PushNameSetting")
public final class PushNameSetting implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used when this setting is encoded in an app
     * state sync mutation.
     */
    public static final String ACTION_NAME = "setting_pushName";

    /**
     * Canonical action version emitted alongside this setting when it is
     * published to the app state sync collection.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * App state collection that stores this setting.
     *
     * <p>Because the push name is visible to every contact and must be
     * available immediately on all devices, it lives in the
     * {@code CRITICAL_BLOCK} patch type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.CRITICAL_BLOCK;

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
     * The user's push name as displayed to contacts.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;


    /**
     * Constructs a new push name setting.
     *
     * @param name the push name, or {@code null} if not set
     */
    PushNameSetting(String name) {
        this.name = name;
    }

    /**
     * Returns the currently configured push name.
     *
     * @return an {@link Optional} containing the push name, or empty if
     *         the setting has never been written
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Updates the push name.
     *
     * @param name the new push name, or {@code null} to clear the value
     */
    public void setName(String name) {
        this.name = name;
    }
}
