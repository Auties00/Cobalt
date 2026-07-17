package com.github.auties00.cobalt.wire.linked.device.info;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Describes the difference between two snapshots of a user's device list.
 *
 * <p>On WhatsApp, every account may be linked to several devices (the primary
 * phone, the web clients, the desktop apps and hosted devices). The list of
 * those devices changes over time whenever a device is linked, unlinked or
 * re-paired. Whenever Cobalt observes a fresh device list for a user it
 * compares it against the previously cached one and produces a
 * {@code DeviceChanges} instance that summarises what changed.
 *
 * <p>The report contains three disjoint sets of device JIDs:
 * <ul>
 *   <li>{@link #addedDevices()}: devices that appear in the new list but
 *   were not present in the previous one. These need a fresh Signal session
 *   before a message can be encrypted for them.</li>
 *   <li>{@link #removedDevices()}: devices that were present in the previous
 *   list but are no longer part of the new one. Existing Signal sessions for
 *   those devices can be discarded.</li>
 *   <li>{@link #identityChangedDevices()}: devices that are still present
 *   but whose identity key index changed, meaning the device was re-paired
 *   and produced a new identity key. Existing sessions for those devices
 *   must be torn down and re-established.</li>
 * </ul>
 *
 * <p>Instances are produced by {@link DeviceList#mismatch(DeviceList)} and
 * are immutable. All accessors return unmodifiable views.
 */
@ProtobufMessage
public final class DeviceChanges {
    /**
     * The set of device JIDs that appeared for the first time in the new
     * device list.
     *
     * <p>Devices listed here require a brand new Signal session to be
     * established before any message can be encrypted for them.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Set<Jid> addedDevices;

    /**
     * The set of device JIDs that were present in the previous device list
     * but are no longer part of the new one.
     *
     * <p>Signal sessions cached for these devices are stale and can be
     * dropped.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Set<Jid> removedDevices;

    /**
     * The set of device JIDs whose identity key rotated between the two
     * snapshots.
     *
     * <p>These devices are still linked to the user but have been re-paired
     * and now use a different identity key. Any existing Signal session for
     * these devices is no longer valid and must be rebuilt.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Set<Jid> identityChangedDevices;

    /**
     * Creates a new change report.
     *
     * <p>This constructor is package-private; instances are normally
     * produced by {@link DeviceList#mismatch(DeviceList)}.
     *
     * @param addedDevices           device JIDs newly added in the new snapshot
     * @param removedDevices         device JIDs that disappeared from the new snapshot
     * @param identityChangedDevices device JIDs whose identity key index changed
     */
    DeviceChanges(Set<Jid> addedDevices, Set<Jid> removedDevices, Set<Jid> identityChangedDevices) {
        this.addedDevices = addedDevices;
        this.removedDevices = removedDevices;
        this.identityChangedDevices = identityChangedDevices;
    }

    /**
     * Returns whether any change of any kind was observed between the two
     * device list snapshots.
     *
     * @return {@code true} if at least one device was added, removed or had
     *         its identity key rotated, {@code false} if the two snapshots
     *         are equivalent
     */
    public boolean hasChanges() {
        return !addedDevices.isEmpty() || !removedDevices.isEmpty() || !identityChangedDevices.isEmpty();
    }

    /**
     * Returns whether any device had its identity key rotated between the
     * two snapshots.
     *
     * <p>This is a useful check before deciding whether outgoing messages
     * need to renegotiate Signal sessions.
     *
     * @return {@code true} if at least one device has a new identity key,
     *         {@code false} otherwise
     */
    public boolean hasIdentityChanges() {
        return !identityChangedDevices.isEmpty();
    }

    /**
     * Returns the device JIDs that are present in the new snapshot but were
     * absent from the previous one.
     *
     * @return an unmodifiable view of the added device JIDs; never
     *         {@code null}
     */
    public Set<Jid> addedDevices() {
        return Collections.unmodifiableSet(addedDevices);
    }

    /**
     * Returns the device JIDs that were present in the previous snapshot
     * but are no longer in the new one.
     *
     * @return an unmodifiable view of the removed device JIDs; never
     *         {@code null}
     */
    public Set<Jid> removedDevices() {
        return Collections.unmodifiableSet(removedDevices);
    }

    /**
     * Returns the device JIDs whose identity key index differs between the
     * two snapshots.
     *
     * @return an unmodifiable view of the device JIDs whose identity key
     *         rotated; never {@code null}
     */
    public Set<Jid> identityChangedDevices() {
        return Collections.unmodifiableSet(identityChangedDevices);
    }

    /**
     * Compares this change report to another object for structural
     * equality.
     *
     * <p>Two reports are equal when they contain the same added, removed
     * and identity changed device JIDs.
     *
     * @param o the object to compare with
     * @return {@code true} if {@code o} is a {@code DeviceChanges} with the
     *         same three sets of device JIDs, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof DeviceChanges that
               && Objects.equals(addedDevices, that.addedDevices)
               && Objects.equals(removedDevices, that.removedDevices)
               && Objects.equals(identityChangedDevices, that.identityChangedDevices);
    }

    /**
     * Returns a hash code derived from the three change sets.
     *
     * @return a hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(addedDevices, removedDevices, identityChangedDevices);
    }

    /**
     * Returns a human readable representation of this change report
     * suitable for logging.
     *
     * @return a string describing the three change sets
     */
    @Override
    public String toString() {
        return "DeviceChanges[" +
               "addedDevices=" + addedDevices + ", " +
               "removedDevices=" + removedDevices + ", " +
               "identityChangedDevices=" + identityChangedDevices + ']';
    }

}
