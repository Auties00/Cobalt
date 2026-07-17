package com.github.auties00.cobalt.wire.linked.message.system.appstate;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

/**
 * Tracks how an app state sync key is bound to the originating device list
 * at the moment the key is generated.
 *
 * <p>When the primary device creates a new app state sync key, it captures
 * a snapshot of the devices that are authorised to receive it: a raw
 * integer identifier for the key, the index of the current (primary)
 * device inside the device list, and the list of device indexes that
 * should receive the key. Companion devices use this fingerprint to
 * detect when their local device list has drifted from the one that was
 * used to encrypt the key and, therefore, when a fresh key distribution
 * is required.
 *
 * <p>The fingerprint is embedded inside {@link AppStateSyncKeyData} and
 * travels with the key whenever it is shared across devices.
 */
@ProtobufMessage(name = "Message.AppStateSyncKeyFingerprint")
public final class AppStateSyncKeyFingerprint implements Message {
    /**
     * The raw numeric identifier assigned to the key at creation time.
     *
     * <p>This is an opaque monotonic value used by the client to
     * distinguish successive key generations.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer rawId;

    /**
     * The index, inside the originating device list, of the device that
     * generated the key.
     *
     * <p>Conventionally this is the primary device index, since only the
     * primary device may distribute new app state sync keys.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer currentIndex;

    /**
     * The indexes of all devices that were part of the user's device list
     * at the time the key was generated.
     *
     * <p>Each entry is a position in the device list, not a device
     * identifier. Companions compare this list against their local view
     * to determine whether the key is still trusted.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32, packed = true)
    List<Integer> deviceIndexes;


    /**
     * Creates a new fingerprint describing how a sync key is bound to a
     * device list.
     *
     * @param rawId         the raw numeric identifier of the key, or
     *                      {@code null} if unset
     * @param currentIndex  the originating device index, or {@code null}
     *                      if unset
     * @param deviceIndexes the device indexes that were authorised to
     *                      receive the key, or {@code null} if unset
     */
    AppStateSyncKeyFingerprint(Integer rawId, Integer currentIndex, List<Integer> deviceIndexes) {
        this.rawId = rawId;
        this.currentIndex = currentIndex;
        this.deviceIndexes = deviceIndexes;
    }

    /**
     * Returns the raw numeric identifier of this key, if present.
     *
     * @return an {@link OptionalInt} containing the identifier, or
     *         {@link OptionalInt#empty()} if no identifier has been set
     */
    public OptionalInt rawId() {
        return rawId == null ? OptionalInt.empty() : OptionalInt.of(rawId);
    }

    /**
     * Returns the index of the device that generated this key, if present.
     *
     * @return an {@link OptionalInt} containing the index, or
     *         {@link OptionalInt#empty()} if no index has been set
     */
    public OptionalInt currentIndex() {
        return currentIndex == null ? OptionalInt.empty() : OptionalInt.of(currentIndex);
    }

    /**
     * Returns the indexes of the devices that were authorised to receive
     * this key at the time it was generated.
     *
     * @return an unmodifiable {@link List} of device indexes, or an empty
     *         list if none has been set
     */
    public List<Integer> deviceIndexes() {
        return deviceIndexes == null ? List.of() : Collections.unmodifiableList(deviceIndexes);
    }

    /**
     * Sets the raw numeric identifier of this key.
     *
     * @param rawId the identifier to assign, or {@code null} to clear
     */
    public void setRawId(Integer rawId) {
        this.rawId = rawId;
    }

    /**
     * Sets the index of the device that generated this key.
     *
     * @param currentIndex the index to assign, or {@code null} to clear
     */
    public void setCurrentIndex(Integer currentIndex) {
        this.currentIndex = currentIndex;
    }

    /**
     * Sets the indexes of the devices that were authorised to receive
     * this key at the time it was generated.
     *
     * @param deviceIndexes the device indexes to assign, or {@code null}
     *                      to clear
     */
    public void setDeviceIndexes(List<Integer> deviceIndexes) {
        this.deviceIndexes = deviceIndexes;
    }
}
