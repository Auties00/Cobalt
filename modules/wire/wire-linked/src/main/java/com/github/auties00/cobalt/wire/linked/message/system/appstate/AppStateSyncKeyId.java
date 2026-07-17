package com.github.auties00.cobalt.wire.linked.message.system.appstate;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Identifies an app state sync key by its opaque binary identifier.
 *
 * <p>App state sync keys are symmetric keys that companion devices use to
 * decrypt the multi-device app state stream (chats, contacts, settings,
 * starred messages, labels, and other synchronised collections). Each key
 * is referenced across the protocol by this identifier, which is generated
 * by the primary device and shared with companions through an
 * {@link AppStateSyncKeyShare} peer message.
 *
 * <p>This type is used both to tag a key when it is distributed (inside
 * {@link AppStateSyncKey}) and to request a specific key by id (inside
 * {@link AppStateSyncKeyRequest}).
 */
@ProtobufMessage(name = "Message.AppStateSyncKeyId")
public final class AppStateSyncKeyId implements Message {
    /**
     * The opaque bytes that uniquely identify an app state sync key.
     *
     * <p>The value is treated as a raw identifier and is not parsed or
     * interpreted; it is only compared for equality when looking up the
     * matching key material on a companion device.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] keyId;


    /**
     * Creates a new app state sync key id with the given identifier bytes.
     *
     * @param keyId the opaque identifier bytes, or {@code null} if unset
     */
    AppStateSyncKeyId(byte[] keyId) {
        this.keyId = keyId;
    }

    /**
     * Returns the opaque identifier bytes of this app state sync key, if present.
     *
     * @return an {@link Optional} containing the identifier bytes, or
     *         {@link Optional#empty()} if no identifier has been set
     */
    public Optional<byte[]> keyId() {
        return Optional.ofNullable(keyId);
    }

    /**
     * Sets the opaque identifier bytes of this app state sync key.
     *
     * @param keyId the identifier bytes to assign, or {@code null} to clear
     */
    public void setKeyId(byte[] keyId) {
        this.keyId = keyId;
    }
}
