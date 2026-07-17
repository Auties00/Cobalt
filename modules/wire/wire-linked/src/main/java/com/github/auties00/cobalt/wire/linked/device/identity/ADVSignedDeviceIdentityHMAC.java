package com.github.auties00.cobalt.wire.linked.device.identity;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Intermediate pairing blob in which a raw device identity is authenticated with a
 * symmetric HMAC rather than with an asymmetric signature.
 *
 * <p>At the very start of device pairing the primary and companion devices have not yet
 * exchanged long-lived identity keys, so they cannot use the asymmetric account signature
 * yet. Instead the primary derives a symmetric secret from the pairing channel, computes
 * {@code HMAC-SHA256(advSecret, details)} over the serialized {@link ADVDeviceIdentity},
 * and sends the bundle to the companion as an {@code ADVSignedDeviceIdentityHMAC}. The
 * companion recomputes the HMAC with its copy of the secret and, if it matches, proceeds
 * to countersign the details and produce a full {@link ADVSignedDeviceIdentity}.
 *
 * <p>Because the HMAC value is only meaningful while the pairing session is alive, this
 * message is never persisted after pairing completes. It only exists to bootstrap trust
 * during the brief interval before the companion has seen the account's public key.
 */
@ProtobufMessage(name = "ADVSignedDeviceIdentityHMAC")
public final class ADVSignedDeviceIdentityHMAC {
    /**
     * Serialized bytes of the {@link ADVDeviceIdentity} being authenticated.
     *
     * <p>These bytes feed both the HMAC computation on the primary side and the HMAC
     * verification on the companion side. They must be preserved verbatim so the
     * companion can reproduce the same tag.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] details;

    /**
     * HMAC tag computed over {@link #details} using the shared ADV secret.
     *
     * <p>The secret is a 32 byte key derived from the pairing flow; it is stored by the
     * primary and by the companion for the duration of pairing and discarded afterwards.
     * The tag defends against tampering while the blob traverses the unencrypted pairing
     * channel.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] hmac;

    /**
     * The encryption scheme selected for the account that is being paired.
     *
     * <p>Optional on the wire, defaults to {@link ADVEncryptionType#E2EE} when absent.
     * The companion uses this value to decide which HMAC domain to apply, matching the
     * hosted variant when a business account opts into coexistence.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    ADVEncryptionType accountType;

    /**
     * Constructs an HMAC-authenticated identity with the given payload. Package-private:
     * use the generated {@code ADVSignedDeviceIdentityHMACBuilder} or the protobuf
     * decoder.
     *
     * @param details     the serialized {@link ADVDeviceIdentity}, or {@code null}
     * @param hmac        the HMAC tag, or {@code null}
     * @param accountType the encryption scheme of the account, or {@code null} for the default
     */
    ADVSignedDeviceIdentityHMAC(byte[] details, byte[] hmac, ADVEncryptionType accountType) {
        this.details = details;
        this.hmac = hmac;
        this.accountType = accountType;
    }

    /**
     * Returns the serialized identity payload being authenticated.
     *
     * @return the raw bytes, or {@link Optional#empty()} when the field was absent
     */
    public Optional<byte[]> details() {
        return Optional.ofNullable(details);
    }

    /**
     * Returns the HMAC tag computed over {@link #details}.
     *
     * @return the tag bytes, or {@link Optional#empty()} when the field was absent
     */
    public Optional<byte[]> hmac() {
        return Optional.ofNullable(hmac);
    }

    /**
     * Returns the encryption scheme selected for the account being paired.
     *
     * @return the account encryption type, or {@link Optional#empty()} when the caller
     *         should treat the value as {@link ADVEncryptionType#E2EE}
     */
    public Optional<ADVEncryptionType> accountType() {
        return Optional.ofNullable(accountType);
    }

    /**
     * Sets the serialized identity payload. The bytes must be treated as immutable once
     * assigned, because any change would invalidate the HMAC.
     *
     * @param details the new payload bytes, or {@code null} to clear
     */
    public void setDetails(byte[] details) {
        this.details = details;
    }

    /**
     * Sets the HMAC tag.
     *
     * @param hmac the new tag bytes, or {@code null} to clear
     */
    public void setHmac(byte[] hmac) {
        this.hmac = hmac;
    }

    /**
     * Sets the encryption scheme of the account being paired.
     *
     * @param accountType the new encryption type, or {@code null} to use the default
     */
    public void setAccountType(ADVEncryptionType accountType) {
        this.accountType = accountType;
    }
}
