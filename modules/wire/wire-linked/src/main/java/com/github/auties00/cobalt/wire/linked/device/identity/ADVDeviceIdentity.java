package com.github.auties00.cobalt.wire.linked.device.identity;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Describes the identity of a single WhatsApp device in the Authenticated Device
 * Verification (ADV) system.
 *
 * <p>Every time a companion device (WhatsApp Web, Desktop, a linked phone, etc.) is
 * paired to a primary account, the primary device issues an {@code ADVDeviceIdentity}
 * that binds the companion to the account. The identity records when the companion was
 * registered, where it sits inside the account's key index list, and which encryption
 * scheme the account and the device use.
 *
 * <p>This structure is not transmitted directly. It is serialized into the
 * {@code details} field of a {@link ADVSignedDeviceIdentity}, which then carries the
 * account and device signatures that prove the device was authorized by the primary
 * account. When a peer receives a message, the attached identity is decoded, both
 * signatures are verified, and the resulting {@code ADVDeviceIdentity} is stored so
 * future messages from the same device can be trusted without re-verifying from scratch.
 *
 * <p>Instances are mutable: each property has a corresponding setter so that fields can
 * be patched after decoding (for example when re-signing the identity during pairing).
 * For normal read access every getter returns an {@link Optional} or {@link OptionalInt}
 * because protobuf fields are optional on the wire.
 */
@ProtobufMessage(name = "ADVDeviceIdentity")
public final class ADVDeviceIdentity {
    /**
     * The raw device identifier assigned by the primary account.
     *
     * <p>Optional on the wire. When present, this is the same identifier used by the
     * account to distinguish companion devices in its device list. It is transmitted as
     * a protobuf {@code uint32} and stored as a boxed {@link Integer} so that absence can
     * be represented as {@code null}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer rawId;

    /**
     * When the device was registered or when this identity was last refreshed.
     *
     * <p>Optional on the wire. Transmitted as a protobuf {@code uint64} holding
     * seconds since the Unix epoch and converted to a Java {@link Instant} via
     * {@link InstantSecondsMixin}. Peers use this timestamp to detect stale device lists
     * and to decide whether an ADV check should be rescheduled.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * The position of this device in its account's signed key index list.
     *
     * <p>Optional on the wire. The account signs a {@link ADVSignedKeyIndexList} that
     * enumerates which key indexes are currently valid. Each companion device is assigned
     * one such index, and this field points back to that entry so a peer can cross check
     * that the device still belongs to the set of authorised companions.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer keyIndex;

    /**
     * The encryption scheme used for the account that owns this device.
     *
     * <p>Optional on the wire, defaults to {@link ADVEncryptionType#E2EE} when absent.
     * Controls which prefix is used when computing or verifying the account signature
     * over the identity details.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    ADVEncryptionType accountType;

    /**
     * The encryption scheme used by this specific device.
     *
     * <p>Optional on the wire, defaults to {@link ADVEncryptionType#E2EE} when absent.
     * The account type and the device type can differ when a business account exposes
     * both end-to-end encrypted and hosted endpoints; the device signature prefix is
     * selected from this value.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    ADVEncryptionType deviceType;

    /**
     * Constructs an identity with the given properties. Package-private: instances are
     * created through the generated {@code ADVDeviceIdentityBuilder} or by the protobuf
     * decoder.
     *
     * @param rawId       the raw device identifier, or {@code null} if unknown
     * @param timestamp   when the device was registered, or {@code null} if unknown
     * @param keyIndex    the position of the device in the signed key index list, or {@code null}
     * @param accountType the encryption scheme used by the account, or {@code null} for the default
     * @param deviceType  the encryption scheme used by the device, or {@code null} for the default
     */
    ADVDeviceIdentity(Integer rawId, Instant timestamp, Integer keyIndex, ADVEncryptionType accountType, ADVEncryptionType deviceType) {
        this.rawId = rawId;
        this.timestamp = timestamp;
        this.keyIndex = keyIndex;
        this.accountType = accountType;
        this.deviceType = deviceType;
    }

    /**
     * Returns the raw device identifier assigned by the account.
     *
     * @return the device id wrapped in an {@link OptionalInt}, or {@link OptionalInt#empty()} if absent
     */
    public OptionalInt rawId() {
        return rawId == null ? OptionalInt.empty() : OptionalInt.of(rawId);
    }

    /**
     * Returns the timestamp at which this device identity was issued or refreshed.
     *
     * @return the timestamp, or {@link Optional#empty()} if the field was not set on the wire
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns the position of this device in its account's signed key index list.
     *
     * @return the key index wrapped in an {@link OptionalInt}, or {@link OptionalInt#empty()} if absent
     */
    public OptionalInt keyIndex() {
        return keyIndex == null ? OptionalInt.empty() : OptionalInt.of(keyIndex);
    }

    /**
     * Returns the encryption scheme of the account that owns this device.
     *
     * @return the account encryption type, or {@link Optional#empty()} when absent; callers
     *         should treat absence as {@link ADVEncryptionType#E2EE}
     */
    public Optional<ADVEncryptionType> accountType() {
        return Optional.ofNullable(accountType);
    }

    /**
     * Returns the encryption scheme used by this specific device.
     *
     * @return the device encryption type, or {@link Optional#empty()} when absent; callers
     *         should treat absence as {@link ADVEncryptionType#E2EE}
     */
    public Optional<ADVEncryptionType> deviceType() {
        return Optional.ofNullable(deviceType);
    }

    /**
     * Sets the raw device identifier.
     *
     * @param rawId the new device id, or {@code null} to clear
     */
    public void setRawId(Integer rawId) {
        this.rawId = rawId;
    }

    /**
     * Sets the identity timestamp.
     *
     * @param timestamp the new timestamp, or {@code null} to clear
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the key index of this device in its account's signed key index list.
     *
     * @param keyIndex the new key index, or {@code null} to clear
     */
    public void setKeyIndex(Integer keyIndex) {
        this.keyIndex = keyIndex;
    }

    /**
     * Sets the encryption scheme of the owning account.
     *
     * @param accountType the new account encryption type, or {@code null} to fall back to the default
     */
    public void setAccountType(ADVEncryptionType accountType) {
        this.accountType = accountType;
    }

    /**
     * Sets the encryption scheme used by this device.
     *
     * @param deviceType the new device encryption type, or {@code null} to fall back to the default
     */
    public void setDeviceType(ADVEncryptionType deviceType) {
        this.deviceType = deviceType;
    }
}
