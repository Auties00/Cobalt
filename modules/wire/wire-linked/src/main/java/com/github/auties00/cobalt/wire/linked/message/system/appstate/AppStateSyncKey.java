package com.github.auties00.cobalt.wire.linked.message.system.appstate;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Pairs an app state sync key identifier with its associated key
 * material.
 *
 * <p>This is the self-contained unit that the primary device shares with
 * its companions so they can decrypt the multi-device app state stream.
 * The {@link AppStateSyncKeyId} identifies the key across the protocol
 * while the {@link AppStateSyncKeyData} payload carries the actual
 * symmetric material, the device list fingerprint and the generation
 * timestamp.
 *
 * <p>One or more such entries are packed inside an
 * {@link AppStateSyncKeyShare} peer message when keys are distributed.
 */
@ProtobufMessage(name = "Message.AppStateSyncKey")
public final class AppStateSyncKey implements Message {
    /**
     * The identifier of the app state sync key carried by this entry.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    AppStateSyncKeyId keyId;

    /**
     * The key material and metadata associated with {@link #keyId}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    AppStateSyncKeyData keyData;


    /**
     * Creates a new app state sync key entry pairing the given id with
     * its key data.
     *
     * @param keyId   the key identifier, or {@code null} if unset
     * @param keyData the key material and metadata, or {@code null} if
     *                unset
     */
    AppStateSyncKey(AppStateSyncKeyId keyId, AppStateSyncKeyData keyData) {
        this.keyId = keyId;
        this.keyData = keyData;
    }

    /**
     * Returns the identifier of this app state sync key, if present.
     *
     * @return an {@link Optional} containing the
     *         {@link AppStateSyncKeyId}, or {@link Optional#empty()} if
     *         no identifier has been set
     */
    public Optional<AppStateSyncKeyId> keyId() {
        return Optional.ofNullable(keyId);
    }

    /**
     * Returns the key material and metadata associated with this entry,
     * if present.
     *
     * @return an {@link Optional} containing the
     *         {@link AppStateSyncKeyData}, or {@link Optional#empty()} if
     *         no payload has been set
     */
    public Optional<AppStateSyncKeyData> keyData() {
        return Optional.ofNullable(keyData);
    }

    /**
     * Sets the identifier of this app state sync key.
     *
     * @param keyId the {@link AppStateSyncKeyId} to assign, or
     *              {@code null} to clear
     */
    public void setKeyId(AppStateSyncKeyId keyId) {
        this.keyId = keyId;
    }

    /**
     * Sets the key material and metadata associated with this entry.
     *
     * @param keyData the {@link AppStateSyncKeyData} to assign, or
     *                {@code null} to clear
     */
    public void setKeyData(AppStateSyncKeyData keyData) {
        this.keyData = keyData;
    }
}
