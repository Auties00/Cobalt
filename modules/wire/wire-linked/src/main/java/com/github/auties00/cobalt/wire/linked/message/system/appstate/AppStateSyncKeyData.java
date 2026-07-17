package com.github.auties00.cobalt.wire.linked.message.system.appstate;

import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Carries the raw material of an app state sync key together with the
 * metadata needed to validate it.
 *
 * <p>An instance bundles the actual symmetric key bytes used to decrypt
 * app state mutations, a {@link AppStateSyncKeyFingerprint} that records
 * the device list snapshot for which the key is valid, and the timestamp
 * at which the key was generated. Together these fields let a companion
 * device decide whether a received key is still trustworthy and when it
 * should be rotated.
 *
 * <p>This payload is always paired with an {@link AppStateSyncKeyId}
 * inside a {@link AppStateSyncKey} entry when the primary device
 * distributes keys to its companions.
 */
@ProtobufMessage(name = "Message.AppStateSyncKeyData")
public final class AppStateSyncKeyData implements Message {
    /**
     * The raw symmetric key bytes used to decrypt app state mutations
     * protected by this key.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] keyData;

    /**
     * The fingerprint binding this key to the device list that was
     * authorised to receive it at generation time.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    AppStateSyncKeyFingerprint fingerprint;

    /**
     * The instant at which this key was generated, expressed in
     * milliseconds since the epoch on the wire.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant timestamp;


    /**
     * Creates a new app state sync key data record with the given
     * material and metadata.
     *
     * @param keyData     the raw symmetric key bytes, or {@code null} if
     *                    unset
     * @param fingerprint the fingerprint describing the authorised
     *                    device list, or {@code null} if unset
     * @param timestamp   the generation timestamp, or {@code null} if
     *                    unset
     */
    AppStateSyncKeyData(byte[] keyData, AppStateSyncKeyFingerprint fingerprint, Instant timestamp) {
        this.keyData = keyData;
        this.fingerprint = fingerprint;
        this.timestamp = timestamp;
    }

    /**
     * Returns the raw symmetric key bytes of this record, if present.
     *
     * @return an {@link Optional} containing the key bytes, or
     *         {@link Optional#empty()} if no material has been set
     */
    public Optional<byte[]> keyData() {
        return Optional.ofNullable(keyData);
    }

    /**
     * Returns the fingerprint that binds this key to a device list
     * snapshot, if present.
     *
     * @return an {@link Optional} containing the
     *         {@link AppStateSyncKeyFingerprint}, or {@link Optional#empty()}
     *         if no fingerprint has been set
     */
    public Optional<AppStateSyncKeyFingerprint> fingerprint() {
        return Optional.ofNullable(fingerprint);
    }

    /**
     * Returns the generation timestamp of this key, if present.
     *
     * @return an {@link Optional} containing the {@link Instant}, or
     *         {@link Optional#empty()} if no timestamp has been set
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Sets the raw symmetric key bytes of this record.
     *
     * @param keyData the key bytes to assign, or {@code null} to clear
     */
    public void setKeyData(byte[] keyData) {
        this.keyData = keyData;
    }

    /**
     * Sets the fingerprint that binds this key to an authorised device
     * list snapshot.
     *
     * @param fingerprint the {@link AppStateSyncKeyFingerprint} to
     *                    assign, or {@code null} to clear
     */
    public void setFingerprint(AppStateSyncKeyFingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Sets the generation timestamp of this key.
     *
     * @param timestamp the {@link Instant} to assign, or {@code null} to
     *                  clear
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
