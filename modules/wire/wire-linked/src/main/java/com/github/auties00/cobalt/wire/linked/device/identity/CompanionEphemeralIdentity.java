package com.github.auties00.cobalt.wire.linked.device.identity;

import com.github.auties00.cobalt.wire.linked.device.pairing.DevicePlatformType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Ephemeral identity that a companion device reveals during the pairing handshake.
 *
 * <p>This is the counterpart of {@link CompanionCommitment}: the companion first sends a
 * hash of this structure, and later opens the commitment by sending the structure itself.
 * It carries everything the primary device needs to continue the Noise handshake, pick
 * the correct pairing user interface for the companion's platform, and correlate the
 * session with an out-of-band pairing reference such as the string encoded in a QR code
 * or the alphanumeric code shown during phone-number pairing.
 */
@ProtobufMessage(name = "CompanionEphemeralIdentity")
public final class CompanionEphemeralIdentity {
    /**
     * Ephemeral Curve25519 public key chosen by the companion for this pairing session.
     *
     * <p>Used as the companion's contribution to the Noise handshake. The key is
     * discarded once pairing completes, so compromise of a paired device cannot retroact
     * to decrypt earlier pairing sessions.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] publicKey;

    /**
     * The platform of the companion device initiating the pairing.
     *
     * <p>Optional on the wire. Lets the primary device display an appropriate confirmation
     * screen ("Link to WhatsApp Web", "Link to Desktop", etc.) and lets downstream logic
     * decide which capability set the companion is allowed to negotiate.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    DevicePlatformType deviceType;

    /**
     * Pairing reference that ties this identity to a specific pairing attempt.
     *
     * <p>Usually the token printed inside the QR code or exchanged through phone-number
     * pairing. The primary uses this string to match an incoming companion identity
     * against the pairing attempt the user actually started on its own screen, rejecting
     * unsolicited identities that do not correspond to any pending reference.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String ref;

    /**
     * Constructs an ephemeral identity with the given properties. Package-private: use
     * the generated {@code CompanionEphemeralIdentityBuilder} or the protobuf decoder.
     *
     * @param publicKey  the ephemeral Curve25519 public key, or {@code null}
     * @param deviceType the companion platform, or {@code null} if unknown
     * @param ref        the pairing reference string, or {@code null}
     */
    CompanionEphemeralIdentity(byte[] publicKey, DevicePlatformType deviceType, String ref) {
        this.publicKey = publicKey;
        this.deviceType = deviceType;
        this.ref = ref;
    }

    /**
     * Returns the ephemeral public key chosen by the companion.
     *
     * @return the key bytes, or {@link Optional#empty()} when the field was absent
     */
    public Optional<byte[]> publicKey() {
        return Optional.ofNullable(publicKey);
    }

    /**
     * Returns the platform of the companion device initiating the pairing.
     *
     * @return the platform type, or {@link Optional#empty()} when the field was not sent
     */
    public Optional<DevicePlatformType> deviceType() {
        return Optional.ofNullable(deviceType);
    }

    /**
     * Returns the pairing reference tied to this identity.
     *
     * @return the reference string, or {@link Optional#empty()} when no reference was
     *         included (for example during in-app pairing flows that do not use one)
     */
    public Optional<String> ref() {
        return Optional.ofNullable(ref);
    }

    /**
     * Sets the ephemeral public key.
     *
     * @param publicKey the new key bytes, or {@code null} to clear
     */
    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Sets the companion platform type.
     *
     * @param deviceType the new platform type, or {@code null} to clear
     */
    public void setDeviceType(DevicePlatformType deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * Sets the pairing reference.
     *
     * @param ref the new reference string, or {@code null} to clear
     */
    public void setRef(String ref) {
        this.ref = ref;
    }
}
