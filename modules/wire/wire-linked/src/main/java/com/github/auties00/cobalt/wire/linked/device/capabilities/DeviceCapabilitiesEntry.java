package com.github.auties00.cobalt.wire.linked.device.capabilities;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model representing the capabilities advertised by a single companion
 * device on this WhatsApp account.
 *
 * <p>Each linked device (Web, Desktop, second phone) advertises a
 * {@link DeviceCapabilities} bag describing the protocol-level features it
 * supports — for example LID-based messaging or new sync-action categories.
 * This entry pairs the device's {@linkplain #deviceJid() JID} with that
 * capability bag so callers can resolve a single device's capabilities
 * without iterating the whole roster of linked devices.
 *
 * <p>Cobalt persists each capability entry independently and refreshes the
 * record whenever the device re-announces its capabilities.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 *
 * @see DeviceCapabilities
 */
@ProtobufMessage
public final class DeviceCapabilitiesEntry {
    /**
     * The non-{@code null} JID of the companion device whose capabilities
     * this entry tracks. Used as the primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid deviceJid;

    /**
     * The non-{@code null} capability bag advertised by the device.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    DeviceCapabilities capabilities;

    /**
     * Constructs a new capability entry pairing the given device JID and
     * capability bag.
     *
     * @param deviceJid    the non-{@code null} device JID
     * @param capabilities the non-{@code null} capability bag
     */
    DeviceCapabilitiesEntry(Jid deviceJid, DeviceCapabilities capabilities) {
        this.deviceJid = Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities cannot be null");
    }

    /**
     * Returns the non-{@code null} device JID.
     *
     * @return the device JID
     */
    public Jid deviceJid() {
        return deviceJid;
    }

    /**
     * Returns the non-{@code null} capability bag advertised by the device.
     *
     * @return the device capabilities
     */
    public DeviceCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Updates the capability bag of this entry.
     *
     * @param capabilities the non-{@code null} new capability bag
     * @return this entry instance for method chaining
     */
    public DeviceCapabilitiesEntry setCapabilities(DeviceCapabilities capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities cannot be null");
        return this;
    }

    /**
     * Returns a hash code derived from this entry's
     * {@linkplain #deviceJid() device JID}.
     *
     * @return the hash code of the device JID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(deviceJid);
    }

    /**
     * Returns whether this capability entry is equal to the given object.
     *
     * <p>Two entries are considered equal when they share the same
     * {@linkplain #deviceJid() device JID}, regardless of the advertised
     * capability bag.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code DeviceCapabilitiesEntry}
     *         with the same device JID
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof DeviceCapabilitiesEntry that && Objects.equals(this.deviceJid, that.deviceJid);
    }
}
