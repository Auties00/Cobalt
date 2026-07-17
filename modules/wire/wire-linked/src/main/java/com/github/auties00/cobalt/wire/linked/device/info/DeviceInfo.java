package com.github.auties00.cobalt.wire.linked.device.info;

import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

import static com.github.auties00.cobalt.wire.linked.device.DeviceConstants.HOSTED_DEVICE_ID;
import static com.github.auties00.cobalt.wire.linked.device.DeviceConstants.PRIMARY_DEVICE_ID;

/**
 * Represents a single device belonging to a WhatsApp account.
 *
 * <p>A WhatsApp account is always paired with a primary device (the phone)
 * and can be linked to several companion devices such as WhatsApp Web,
 * Desktop or tablets, plus an optional hosted device used by WhatsApp for
 * business integrations. Each device is identified by a small numeric
 * device id and carries an identity key index that uniquely describes the
 * current identity key of the device.
 *
 * <p>A {@code DeviceInfo} holds the minimal information needed to address
 * and authenticate a device when sending messages through the Signal
 * protocol:
 * <ul>
 *   <li>the device id inside its user namespace (0 for the primary phone,
 *   a small integer for each companion, 99 for a hosted device);</li>
 *   <li>the identity key index, which rotates every time the device is
 *   re-paired and is used to detect identity changes;</li>
 *   <li>the Advanced (ADV) encryption type of the device, which determines
 *   how the device participates in the protocol;</li>
 *   <li>an explicit flag telling whether the device is hosted, used to
 *   support cases where the id alone is not sufficient.</li>
 * </ul>
 *
 * <p>Instances are immutable and typically grouped inside a
 * {@link DeviceList}. Use {@link #toDeviceJid(String, JidServer)} to turn a
 * {@code DeviceInfo} into a full addressable {@link Jid}.
 */
@ProtobufMessage
public final class DeviceInfo {
    /**
     * The numeric device id inside the owning user account.
     *
     * <p>Conventional values are:
     * <ul>
     *   <li>{@code 0}: the primary device (the phone);</li>
     *   <li>{@code 1} to {@code 98}: companion devices such as WhatsApp
     *   Web, Desktop or linked tablets;</li>
     *   <li>{@code 99}: a hosted device used for business integrations.</li>
     * </ul>
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final int id;

    /**
     * The identity key index of the device.
     *
     * <p>The identity key index is an integer that uniquely identifies the
     * current long term identity key of the device and is bumped every time
     * the device is re-paired. Comparing the key index of two snapshots of
     * the same device is how identity key rotations are detected. A value
     * of {@code -1} or similar negative sentinel means that the key index
     * is unknown.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final int keyIndex;

    /**
     * The Advanced encryption type of the device.
     *
     * <p>Describes how the device takes part in the end to end encryption
     * protocol. Typical values are {@link ADVEncryptionType#E2EE} for
     * regular end to end encrypted devices and
     * {@link ADVEncryptionType#HOSTED} for hosted business devices.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final ADVEncryptionType type;

    /**
     * Explicit marker that identifies the device as a hosted device.
     *
     * <p>In newer USync protocol versions hosted devices can have arbitrary
     * ids and are flagged with a dedicated {@code is_hosted} attribute
     * rather than relying on the conventional id {@code 99}. This flag
     * mirrors that attribute so that the information is preserved when a
     * device is parsed from the server.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean hostedFlag;

    /**
     * Creates a new {@code DeviceInfo} with the provided attributes.
     *
     * <p>This constructor is package-private; clients should use one of
     * the {@code ofE2EE} or {@code ofHosted} factory methods instead.
     *
     * @param id         the device id
     * @param keyIndex   the identity key index
     * @param type       the ADV encryption type of the device
     * @param hostedFlag whether the device is explicitly flagged as hosted
     */
    DeviceInfo(int id, int keyIndex, ADVEncryptionType type, boolean hostedFlag) {
        this.id = id;
        this.keyIndex = keyIndex;
        this.type = type;
        this.hostedFlag = hostedFlag;
    }

    /**
     * Creates a {@code DeviceInfo} for a regular end to end encrypted
     * device.
     *
     * <p>The device is marked as non-hosted and tagged with
     * {@link ADVEncryptionType#E2EE}.
     *
     * @param id       the device id
     * @param keyIndex the identity key index reported for the device
     * @return a new {@code DeviceInfo} describing a regular E2EE device
     */
    public static DeviceInfo ofE2EE(int id, int keyIndex) {
        return new DeviceInfo(id, keyIndex, ADVEncryptionType.E2EE, false);
    }

    /**
     * Creates a {@code DeviceInfo} for an end to end encrypted device with
     * an explicit hosted flag.
     *
     * <p>Used when parsing USync responses where the {@code is_hosted}
     * attribute is carried alongside the device id and must be preserved
     * verbatim.
     *
     * @param id         the device id
     * @param keyIndex   the identity key index reported for the device
     * @param hostedFlag whether the device should be flagged as hosted
     * @return a new {@code DeviceInfo} describing an E2EE device with the
     *         given hosted flag
     */
    public static DeviceInfo ofE2EE(int id, int keyIndex, boolean hostedFlag) {
        return new DeviceInfo(id, keyIndex, ADVEncryptionType.E2EE, hostedFlag);
    }

    /**
     * Creates a {@code DeviceInfo} for a hosted device.
     *
     * <p>The device is assigned the conventional hosted device id
     * ({@code 99}), tagged with {@link ADVEncryptionType#HOSTED} and has
     * the hosted flag enabled.
     *
     * @param keyIndex the identity key index reported for the hosted device
     * @return a new {@code DeviceInfo} describing a hosted device
     */
    public static DeviceInfo ofHosted(int keyIndex) {
        return new DeviceInfo(HOSTED_DEVICE_ID, keyIndex, ADVEncryptionType.HOSTED, true);
    }

    /**
     * Returns whether this device is a hosted device.
     *
     * <p>A device is considered hosted when it uses the conventional
     * hosted device id ({@code 99}) or when it has been explicitly flagged
     * as hosted by the server via the {@code is_hosted} attribute.
     *
     * @return {@code true} if the device is hosted, {@code false} otherwise
     */
    public boolean isHosted() {
        return id == HOSTED_DEVICE_ID || hostedFlag;
    }

    /**
     * Returns the raw hosted flag reported by the server, without taking
     * the device id into account.
     *
     * <p>For most use cases {@link #isHosted()} should be preferred.
     *
     * @return the value of the {@code is_hosted} attribute as received
     *         from the server
     */
    public boolean hostedFlag() {
        return hostedFlag;
    }

    /**
     * Returns whether this device is the primary device of its account.
     *
     * <p>The primary device has the conventional id {@code 0} and is
     * typically the phone on which the account was originally registered.
     *
     * @return {@code true} if the device id equals {@code 0},
     *         {@code false} otherwise
     */
    public boolean isPrimary() {
        return id == PRIMARY_DEVICE_ID;
    }

    /**
     * Returns whether this device is a companion device.
     *
     * <p>Companion devices are non-primary, non-hosted devices such as a
     * linked WhatsApp Web, Desktop or tablet; their id is strictly between
     * {@code 0} and {@code 99}.
     *
     * @return {@code true} if this is a companion device, {@code false}
     *         for the primary device or a hosted device
     */
    public boolean isCompanion() {
        return id > PRIMARY_DEVICE_ID && id < HOSTED_DEVICE_ID;
    }

    /**
     * Returns the full addressable JID of this device.
     *
     * <p>Combines the user portion, the JID server and this device's id
     * into a device scoped {@link Jid} that can be used to send messages
     * to the specific device.
     *
     * @param user   the user part of the owning account
     * @param server the JID server of the owning account
     * @return the JID that addresses this device inside the owning account
     */
    public Jid toDeviceJid(String user, JidServer server) {
        return Jid.of(user, server, id, 0);
    }

    /**
     * Returns the numeric device id.
     *
     * <p>See {@link #id} for the meaning of the conventional values.
     *
     * @return the device id
     */
    public int id() {
        return id;
    }

    /**
     * Returns the identity key index of the device.
     *
     * <p>See {@link #keyIndex} for details about what the value represents.
     *
     * @return the identity key index
     */
    public int keyIndex() {
        return keyIndex;
    }

    /**
     * Returns the ADV encryption type of the device.
     *
     * @return the encryption type of the device
     */
    public ADVEncryptionType type() {
        return type;
    }

    /**
     * Compares this device info to another object for structural equality.
     *
     * <p>Two {@code DeviceInfo} instances are equal when they share the
     * same id, identity key index, encryption type and hosted flag.
     *
     * @param o the object to compare with
     * @return {@code true} if {@code o} is a {@code DeviceInfo} with the
     *         same attributes, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof DeviceInfo that
                            && id == that.id
                            && keyIndex == that.keyIndex
                            && type == that.type
                            && hostedFlag == that.hostedFlag;
    }

    /**
     * Returns a hash code derived from the device attributes.
     *
     * @return a hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, keyIndex, type, hostedFlag);
    }

    /**
     * Returns a human readable representation of this device info suitable
     * for logging.
     *
     * @return a string describing the device
     */
    @Override
    public String toString() {
        return "DeviceInfo[" +
               "id=" + id + ", " +
               "keyIndex=" + keyIndex + ", " +
               "type=" + type + ", " +
               "hostedFlag=" + hostedFlag + ']';
    }


}
