package com.github.auties00.cobalt.wire.linked.message.system.appstate;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Peer message carrying a batch of app state sync keys from the primary
 * device to one of its companions.
 *
 * <p>Whenever new app state sync keys are generated (for example the
 * first time a companion device is linked, or when the device list
 * changes and keys must be rotated), the primary device bundles one or
 * more {@link AppStateSyncKey} entries inside this message and sends it
 * as an end-to-end encrypted peer message. Once received, the companion
 * stores each key under its {@link AppStateSyncKeyId} and can decrypt
 * incoming app state mutations encrypted with that key.
 */
@ProtobufMessage(name = "Message.AppStateSyncKeyShare")
public final class AppStateSyncKeyShare implements Message {
    /**
     * The app state sync keys being shared in this batch.
     *
     * <p>Each entry pairs an {@link AppStateSyncKeyId} with its
     * corresponding {@link AppStateSyncKeyData} payload.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<AppStateSyncKey> keys;


    /**
     * Creates a new share message carrying the given app state sync keys.
     *
     * @param keys the keys to distribute, or {@code null} if unset
     */
    AppStateSyncKeyShare(List<AppStateSyncKey> keys) {
        this.keys = keys;
    }

    /**
     * Returns the app state sync keys carried by this share message.
     *
     * @return an unmodifiable {@link List} of {@link AppStateSyncKey}
     *         entries, or an empty list if no keys have been set
     */
    public List<AppStateSyncKey> keys() {
        return keys == null ? List.of() : Collections.unmodifiableList(keys);
    }

    /**
     * Sets the app state sync keys carried by this share message.
     *
     * @param keys the keys to distribute, or {@code null} to clear
     */
    public void setKeys(List<AppStateSyncKey> keys) {
        this.keys = keys;
    }
}
