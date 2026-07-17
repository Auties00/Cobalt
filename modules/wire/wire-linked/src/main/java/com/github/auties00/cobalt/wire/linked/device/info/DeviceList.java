package com.github.auties00.cobalt.wire.linked.device.info;

import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the cached list of all devices linked to a WhatsApp account.
 *
 * <p>Every WhatsApp account may be linked to a primary device (the phone),
 * several companion devices (WhatsApp Web, Desktop, tablets) and an
 * optional hosted device. When Cobalt wants to send a message to a user it
 * must know every device of that user so that the payload is encrypted
 * once per recipient device. The {@code DeviceList} is the in-memory and
 * persisted representation of that information.
 *
 * <p>A device list is fetched from the server through the USync protocol
 * and cached locally. It is updated whenever a notification is received
 * about a device being added, removed or re-paired. The cache is also used
 * as input to the Identity Change Detection Consistency (ICDC) algorithm,
 * which produces a hash of all known identity keys and sends it in every
 * outgoing message so that the recipient can detect when its view of the
 * sender's devices is out of date.
 *
 * <p>A {@code DeviceList} carries, alongside the actual {@link DeviceInfo}
 * entries:
 * <ul>
 *   <li>the JID of the user that owns the devices;</li>
 *   <li>the server timestamp at which the list was computed, used as a
 *   freshness indicator and to negotiate delta updates;</li>
 *   <li>expected timestamp fields used by the server to reconcile the
 *   client's view with its own;</li>
 *   <li>soft delete markers and the ADV account type of the user;</li>
 *   <li>the current index and the set of valid indexes of the signed key
 *   index list published by the primary device.</li>
 * </ul>
 *
 * <p>Instances are immutable; {@link #withUserJid(Jid)} returns a new value
 * rather than updating the receiver in place, and {@link #mismatch(DeviceList)}
 * reports differences without mutating either operand.
 */
@ProtobufMessage
public final class DeviceList {
    /**
     * The user JID that owns this device list.
     *
     * <p>Contains only the user portion; each individual device is
     * addressed by combining this JID with a {@link DeviceInfo#id()}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid userJid;

    /**
     * The devices currently linked to the user.
     *
     * <p>Stored as an unmodifiable list. The list contains at most one
     * primary device and at most one hosted device; the remaining entries
     * describe companion devices.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<DeviceInfo> devices;

    /**
     * The timestamp at which the server produced this device list.
     *
     * <p>Used as a freshness indicator when reconciling with the server
     * through USync and when computing ICDC metadata attached to outgoing
     * messages.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant timestamp;

    /**
     * The raw identifier under which this device list is stored in the
     * WhatsApp Web IndexedDB device list table.
     *
     * <p>Preserved verbatim so that Cobalt can interoperate with data
     * originally produced by WhatsApp Web.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String rawId;

    /**
     * Whether this device list has been marked as deleted.
     *
     * <p>A deleted device list is kept in cache as a tombstone so that
     * subsequent fetches do not resurrect it unless the server explicitly
     * sends a new list. Deleted lists must be ignored by outgoing message
     * flows and by ICDC computation.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean deleted;

    /**
     * Whether this device list was deleted specifically because the user
     * account was converted into a hosted account.
     *
     * <p>Differentiates between a regular deletion and a deletion caused
     * by an account type switch so that the right recovery path can be
     * selected when a new device list is eventually received.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    final boolean deletedChangedToHost;

    /**
     * The ADV encryption type of the account that owns this device list.
     *
     * <p>Describes the global encryption regime of the user (for example
     * {@link ADVEncryptionType#E2EE} or {@link ADVEncryptionType#HOSTED}).
     * May be {@code null} when the value is unknown.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    final ADVEncryptionType advAccountType;

    /**
     * The timestamp that the server expects the next version of the device
     * list to carry.
     *
     * <p>Used by the server as a hint to detect whether a client is out of
     * date. May be {@code null} when no expectation has been communicated.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant expectedTimestamp;

    /**
     * The timestamp at which a device list job last attempted to update
     * {@link #expectedTimestamp}.
     *
     * <p>Used internally by the background job that keeps the expected
     * timestamp in sync with the server to avoid redundant work.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant expectedTimestampLastDeviceJobTimestamp;

    /**
     * The timestamp at which {@link #expectedTimestamp} was last updated.
     *
     * <p>Preserves the last moment the expected timestamp was actually
     * refreshed, which may differ from the last time the update job ran.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant expectedTimestampUpdateTimestamp;

    /**
     * The current index into the signed key index list published by the
     * account's primary device.
     *
     * <p>Corresponds to the index that the server currently considers
     * authoritative for this account.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT32)
    final int currentIndex;

    /**
     * The set of indexes that are currently considered valid in the
     * signed key index list published by the account's primary device.
     *
     * <p>Ordered to preserve the insertion order reported by the server.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.UINT32)
    final SequencedSet<Integer> validIndexes;

    /**
     * Creates a new device list with all of its fields set.
     *
     * <p>This constructor is package-private; clients should use
     * {@code DeviceListBuilder} to construct instances. Passing
     * {@code null} for {@code devices} results in an empty list. Passing
     * {@code null} for {@code validIndexes} results in an empty
     * {@link LinkedHashSet}.
     *
     * @param userJid                                  the user JID that owns the list
     * @param devices                                  the devices currently linked to the user
     * @param timestamp                                the timestamp at which the list was computed
     * @param rawId                                    the raw identifier under which the list is stored
     * @param deleted                                  whether the list is marked as deleted
     * @param deletedChangedToHost                     whether the deletion is due to an account type change
     * @param advAccountType                           the ADV encryption type of the account
     * @param expectedTimestamp                        the expected next timestamp of the list
     * @param expectedTimestampLastDeviceJobTimestamp  the time the expected timestamp update job last ran
     * @param expectedTimestampUpdateTimestamp         the time the expected timestamp was last updated
     * @param currentIndex                             the current index in the signed key index list
     * @param validIndexes                             the valid indexes in the signed key index list
     */
    DeviceList(
            Jid userJid,
            List<DeviceInfo> devices,
            Instant timestamp,
            String rawId,
            boolean deleted,
            boolean deletedChangedToHost,
            ADVEncryptionType advAccountType,
            Instant expectedTimestamp,
            Instant expectedTimestampLastDeviceJobTimestamp,
            Instant expectedTimestampUpdateTimestamp,
            int currentIndex,
            SequencedSet<Integer> validIndexes
    ) {
        this.userJid = userJid;
        this.devices = devices != null ? List.copyOf(devices) : List.of();
        this.timestamp = timestamp;
        this.rawId = rawId;
        this.deleted = deleted;
        this.deletedChangedToHost = deletedChangedToHost;
        this.advAccountType = advAccountType;
        this.expectedTimestamp = expectedTimestamp;
        this.expectedTimestampLastDeviceJobTimestamp = expectedTimestampLastDeviceJobTimestamp;
        this.expectedTimestampUpdateTimestamp = expectedTimestampUpdateTimestamp;
        this.currentIndex = currentIndex;
        this.validIndexes = Objects.requireNonNullElseGet(validIndexes, LinkedHashSet::new);
    }

    /**
     * Returns the user JID that owns this device list.
     *
     * @return the user JID
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns an unmodifiable view of the devices currently linked to the
     * user.
     *
     * @return the list of device entries; never {@code null}
     */
    public List<DeviceInfo> devices() {
        return devices;
    }

    /**
     * Returns the timestamp at which this device list was produced.
     *
     * @return the device list timestamp
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns the raw identifier under which the list is stored.
     *
     * @return the raw id, or {@code null} if unknown
     */
    public String rawId() {
        return rawId;
    }

    /**
     * Returns whether this device list is marked as deleted.
     *
     * @return {@code true} if deleted, {@code false} otherwise
     */
    public boolean deleted() {
        return deleted;
    }

    /**
     * Returns whether the deletion of this list was triggered by an
     * account type switch.
     *
     * @return {@code true} if the deletion is caused by a conversion of
     *         the account to a hosted account, {@code false} otherwise
     */
    public boolean deletedChangedToHost() {
        return deletedChangedToHost;
    }

    /**
     * Returns the ADV encryption type of the account that owns this list.
     *
     * @return the ADV account type, or {@code null} if unknown
     */
    public ADVEncryptionType advAccountType() {
        return advAccountType;
    }

    /**
     * Returns the timestamp that the server expects the next version of
     * the list to carry.
     *
     * @return the expected timestamp, or {@code null} if unknown
     */
    public Instant expectedTimestamp() {
        return expectedTimestamp;
    }

    /**
     * Returns the time at which the expected timestamp update job last
     * ran for this list.
     *
     * @return the last run time, or {@code null} if unknown
     */
    public Instant expectedTimestampLastDeviceJobTimestamp() {
        return expectedTimestampLastDeviceJobTimestamp;
    }

    /**
     * Returns the time at which the expected timestamp was last updated.
     *
     * @return the last update time, or {@code null} if unknown
     */
    public Instant expectedTimestampUpdateTimestamp() {
        return expectedTimestampUpdateTimestamp;
    }

    /**
     * Returns the current index in the signed key index list published by
     * the account's primary device.
     *
     * @return the current index
     */
    public int currentIndex() {
        return currentIndex;
    }

    /**
     * Returns an unmodifiable view of the valid indexes in the signed key
     * index list published by the account's primary device.
     *
     * @return the valid indexes in their insertion order; never
     *         {@code null}
     */
    public SequencedSet<Integer> validIndexes() {
        return Collections.unmodifiableSequencedSet(validIndexes);
    }

    /**
     * Returns the primary device of the account, if present.
     *
     * <p>The primary device is the one whose id equals {@code 0}. In a
     * well formed device list exactly one such entry is expected, but a
     * stale or partial list may be missing it.
     *
     * @return an {@link Optional} holding the primary device when present,
     *         otherwise {@link Optional#empty()}
     */
    public Optional<DeviceInfo> primaryDevice() {
        return devices.stream()
                .filter(DeviceInfo::isPrimary)
                .findFirst();
    }

    /**
     * Returns all devices except the hosted one.
     *
     * <p>The result contains the primary device together with every
     * companion device. It excludes the hosted device because hosted
     * devices are typically handled through a different message flow.
     *
     * @return the devices that are not hosted
     */
    public List<DeviceInfo> e2eeDevices() {
        return devices.stream()
                .filter(d -> !d.isHosted())
                .toList();
    }

    /**
     * Returns the hosted device of the account, if present.
     *
     * <p>An account has at most one hosted device.
     *
     * @return an {@link Optional} holding the hosted device when present,
     *         otherwise {@link Optional#empty()}
     */
    public Optional<DeviceInfo> hostedDevices() {
        return devices.stream()
                .filter(DeviceInfo::isHosted)
                .findFirst();
    }

    /**
     * Returns the full addressable JIDs of every device in the list.
     *
     * <p>Each JID is built from {@link #userJid} and the id of the
     * corresponding {@link DeviceInfo}.
     *
     * @return an unmodifiable set of device JIDs
     */
    public Set<Jid> deviceJids() {
        return devices.stream()
                .map(d -> d.toDeviceJid(userJid.user(), userJid.server()))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the number of devices in this list.
     *
     * @return the device count
     */
    public int size() {
        return devices.size();
    }

    /**
     * Returns whether this list contains no devices.
     *
     * <p>Note that an empty list is distinct from a deleted list; see
     * {@link #deleted()}.
     *
     * @return {@code true} if the list is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return devices.isEmpty();
    }

    /**
     * Returns whether the ADV account type has changed between this list
     * and another list.
     *
     * <p>Useful to detect, for example, a transition from a regular E2EE
     * account to a hosted account, which may require extra bookkeeping.
     *
     * @param other the other device list to compare with
     * @return {@code true} if both lists specify an account type and the
     *         two values differ, {@code false} otherwise
     */
    public boolean hasAccountTypeChanged(DeviceList other) {
        return other != null
                && advAccountType != null
                && other.advAccountType != null
                && advAccountType != other.advAccountType;
    }

    /**
     * Returns the differences between this device list and a previous
     * snapshot.
     *
     * <p>When {@code other} is {@code null} every device currently in this
     * list is reported as added. Otherwise the two lists are compared
     * device by device:
     * <ul>
     *   <li>a device present here but not in {@code other} is reported as
     *   added;</li>
     *   <li>a device present in {@code other} but not here is reported as
     *   removed;</li>
     *   <li>a device present on both sides but with a different identity
     *   key index is reported as having rotated its identity key.</li>
     * </ul>
     *
     * <p>The resulting {@link DeviceChanges} can be used to refresh Signal
     * sessions and retry message delivery when a device list changes.
     *
     * @param other the previous device list snapshot; {@code null} means
     *              no previous snapshot is known
     * @return a change report describing the differences between the two
     *         snapshots
     */
    public DeviceChanges mismatch(DeviceList other) {
        if (other == null) {
            return new DeviceChanges(deviceJids(), Set.of(), Set.of());
        }

        var otherDevices = new HashMap<Integer, DeviceInfo>();
        for (var device : other.devices) {
            otherDevices.put(device.id(), device);
        }

        var added = new HashSet<Jid>();
        var identityChanged = new HashSet<Jid>();

        for (var device : devices) {
            var otherDevice = otherDevices.remove(device.id());
            var deviceJid = device.toDeviceJid(userJid.user(), userJid.server());

            if (otherDevice == null) {
                added.add(deviceJid);
            } else if (otherDevice.keyIndex() >= 0 && otherDevice.keyIndex() != device.keyIndex()) {
                identityChanged.add(deviceJid);
            }
        }

        var removed = otherDevices.values().stream()
                .map(d -> d.toDeviceJid(userJid.user(), userJid.server()))
                .collect(Collectors.toUnmodifiableSet());

        return new DeviceChanges(added, removed, identityChanged);
    }

    /**
     * Returns a copy of this device list owned by {@code userJid}, or this same instance when it
     * already carries that JID.
     *
     * <p>Only the owner JID changes; the device entries, timestamps, deletion markers, account
     * type, and signed-key-index metadata are preserved verbatim. Each device JID is minted by
     * combining {@link #userJid()} with a device id (see {@link #deviceJids()}), and device ids are
     * independent of the addressing mode, so re-keying to a different addressing of the same account
     * (for example the LID counterpart of a phone-number JID) renders the same devices under that
     * addressing.
     *
     * @param userJid the new owner JID
     * @return a copy owned by {@code userJid}, or this instance when {@code userJid} already owns it
     */
    public DeviceList withUserJid(Jid userJid) {
        if (Objects.equals(this.userJid, userJid)) {
            return this;
        }
        return new DeviceList(
                userJid,
                devices,
                timestamp,
                rawId,
                deleted,
                deletedChangedToHost,
                advAccountType,
                expectedTimestamp,
                expectedTimestampLastDeviceJobTimestamp,
                expectedTimestampUpdateTimestamp,
                currentIndex,
                validIndexes
        );
    }

    /**
     * Compares this device list to another object for structural equality.
     *
     * <p>Two device lists are equal when all of their fields match
     * pairwise, including the owner JID, the devices, the timestamps and
     * every piece of metadata.
     *
     * @param o the object to compare with
     * @return {@code true} if {@code o} is a {@code DeviceList} equal to
     *         {@code this}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof DeviceList other
                && Objects.equals(userJid, other.userJid)
                && Objects.equals(devices, other.devices)
                && Objects.equals(timestamp, other.timestamp)
                && Objects.equals(rawId, other.rawId)
                && deleted == other.deleted
                && deletedChangedToHost == other.deletedChangedToHost
                && advAccountType == other.advAccountType
                && Objects.equals(expectedTimestamp, other.expectedTimestamp)
                && Objects.equals(expectedTimestampLastDeviceJobTimestamp, other.expectedTimestampLastDeviceJobTimestamp)
                && Objects.equals(expectedTimestampUpdateTimestamp, other.expectedTimestampUpdateTimestamp)
                && currentIndex == other.currentIndex
                && Objects.equals(validIndexes, other.validIndexes);
    }

    /**
     * Returns a hash code derived from every field of this device list.
     *
     * @return a hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(userJid, devices, timestamp, rawId, deleted,
                deletedChangedToHost, advAccountType, expectedTimestamp,
                expectedTimestampLastDeviceJobTimestamp, expectedTimestampUpdateTimestamp,
                currentIndex, validIndexes);
    }

    /**
     * Returns a compact human readable representation of this device list
     * suitable for logging.
     *
     * <p>The rendered string is intentionally concise; it exposes the
     * owner JID, the number of devices and the deletion flag rather than
     * dumping every field.
     *
     * @return a short descriptive string
     */
    @Override
    public String toString() {
        return "DeviceList[userJid=" + userJid + ", devices=" + devices.size() + ", deleted=" + deleted + "]";
    }
}
