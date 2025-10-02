
package it.auties.whatsapp.api;

import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPrivateKey;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Represents the six-part authentication keys used for WhatsApp mobile client connections.
 * <p>
 * This class encapsulates the cryptographic credentials required to authenticate a WhatsApp mobile
 * client session. The six parts consist of:
 * <ol>
 *     <li>Phone number (with optional '+' prefix)</li>
 *     <li>Noise protocol public key (Base64 encoded)</li>
 *     <li>Noise protocol private key (Base64 encoded)</li>
 *     <li>Signal identity public key (Base64 encoded)</li>
 *     <li>Signal identity private key (Base64 encoded)</li>
 *     <li>Identity ID (Base64 encoded)</li>
 * </ol>
 * <p>
 * These credentials are typically obtained from an existing WhatsApp mobile installation and can be
 * used to restore a session or create a new connection using {@link WhatsappBuilder}.
 * <p>
 * The six parts must be provided as a comma-separated string, with optional whitespace and newlines
 * that will be automatically stripped during parsing.
 * <p>
 *
 * @see WhatsappBuilder
 * @see SignalIdentityKeyPair
 */
public final class WhatsappSixPartsKeys {
    private final long phoneNumber;
    private final SignalIdentityKeyPair noiseKeyPair;
    private final SignalIdentityKeyPair identityKeyPair;
    private final byte[] identityId;

    /**
     * Constructs a new WhatsappSixPartsKeys instance with the specified components.
     *
     * @param phoneNumber      the phone number associated with the WhatsApp account
     * @param noiseKeyPair     the Noise protocol key pair used for secure channel establishment
     * @param identityKeyPair  the Signal identity key pair used for end-to-end encryption
     * @param identityId       the unique identity identifier for this account
     */
    private WhatsappSixPartsKeys(long phoneNumber, SignalIdentityKeyPair noiseKeyPair, SignalIdentityKeyPair identityKeyPair, byte[] identityId) {
        this.phoneNumber = phoneNumber;
        this.noiseKeyPair = noiseKeyPair;
        this.identityKeyPair = identityKeyPair;
        this.identityId = identityId;
    }

    /**
     * Parses a six-parts authentication string and creates a WhatsappSixPartsKeys instance.
     * <p>
     * The input string must contain exactly six comma-separated parts in the following order:
     * <ol>
     *     <li>Phone number (with optional '+' prefix)</li>
     *     <li>Noise public key (Base64 encoded)</li>
     *     <li>Noise private key (Base64 encoded)</li>
     *     <li>Identity public key (Base64 encoded)</li>
     *     <li>Identity private key (Base64 encoded)</li>
     *     <li>Identity ID (Base64 encoded)</li>
     * </ol>
     * <p>
     * Whitespace and newlines are automatically stripped from the input string.
     *
     * @param sixParts the comma-separated six-parts authentication string
     * @return a new WhatsappSixPartsKeys instance containing the parsed credentials
     * @throws NullPointerException     if sixParts is null
     * @throws IllegalArgumentException if the string format is invalid, doesn't contain exactly six parts,
     *                                  or the phone number is malformed
     */
    public static WhatsappSixPartsKeys of(String sixParts) {
        Objects.requireNonNull(sixParts, "Invalid six parts");
        var parts = sixParts.trim()
                .replaceAll("\n", "")
                .split(",", 6);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Malformed six parts: " + sixParts);
        }
        var phoneNumber = parsePhoneNumber(parts);
        var noisePublicKey = SignalIdentityPublicKey.ofDirect(Base64.getDecoder().decode(parts[1]));
        var noisePrivateKey = SignalIdentityPrivateKey.ofDirect(Base64.getDecoder().decode(parts[2]));
        var identityPublicKey = SignalIdentityPublicKey.ofDirect(Base64.getDecoder().decode(parts[3]));
        var identityPrivateKey = SignalIdentityPrivateKey.ofDirect(Base64.getDecoder().decode(parts[4]));
        var identityId = Base64.getDecoder().decode(parts[5]);
        var noiseKeyPair = new SignalIdentityKeyPair(noisePublicKey, noisePrivateKey);
        var identityKeyPair = new SignalIdentityKeyPair(identityPublicKey, identityPrivateKey);
        return new WhatsappSixPartsKeys(phoneNumber, noiseKeyPair, identityKeyPair, identityId);
    }

    /**
     * Parses the phone number from the first part of the six-parts array.
     *
     * @param parts the array of six parts
     * @return the phone number as a long
     * @throws IllegalArgumentException if the phone number is empty or contains invalid characters
     */
    private static long parsePhoneNumber(String[] parts) {
        var rawPhoneNumber = parts[0];
        if(rawPhoneNumber.isEmpty()) {
            throw new IllegalArgumentException("Invalid phone number: " + rawPhoneNumber);
        }
        try {
            return Long.parseUnsignedLong(rawPhoneNumber, rawPhoneNumber.charAt(0) == '+' ? 1 : 0, rawPhoneNumber.length(), 10);
        }catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid phone number: " + rawPhoneNumber);
        }
    }

    /**
     * Converts this WhatsappSixPartsKeys instance back to its six-parts string representation.
     * <p>
     * The returned string contains six comma-separated Base64-encoded parts that can be used to
     * reconstruct this instance using {@link #of(String)}.
     *
     * @return the six-parts string representation of these credentials
     */
    @Override
    public String toString() {
        return String.valueOf(phoneNumber) +
                ',' +
                Base64.getEncoder().encodeToString(noiseKeyPair.publicKey().toEncodedPoint()) +
                ',' +
                Base64.getEncoder().encodeToString(noiseKeyPair.privateKey().toEncodedPoint()) +
                ',' +
                Base64.getEncoder().encodeToString(identityKeyPair.publicKey().toEncodedPoint()) +
                ',' +
                Base64.getEncoder().encodeToString(identityKeyPair.privateKey().toEncodedPoint()) +
                ',' +
                Base64.getEncoder().encodeToString(identityId);
    }

    /**
     * Returns the phone number associated with these credentials.
     *
     * @return the phone number as a long value
     */
    public long phoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns the Noise protocol key pair used for secure channel establishment.
     * <p>
     * The Noise protocol is used during the initial handshake phase to establish an encrypted
     * connection with WhatsApp servers.
     *
     * @return the Noise protocol key pair
     */
    public SignalIdentityKeyPair noiseKeyPair() {
        return noiseKeyPair;
    }

    /**
     * Returns the Signal identity key pair used for end-to-end encryption.
     * <p>
     * This key pair is used for the Signal protocol implementation that provides end-to-end
     * encryption for all WhatsApp messages.
     *
     * @return the Signal identity key pair
     */
    public SignalIdentityKeyPair identityKeyPair() {
        return identityKeyPair;
    }

    /**
     * Returns the unique identity identifier for this account.
     * <p>
     * Note: The returned array is the actual internal array, not a copy. Modifications to the
     * returned array will affect this instance.
     *
     * @return the identity ID as a byte array
     */
    public byte[] identityId() {
        return identityId;
    }

    /**
     * Compares this WhatsappSixPartsKeys instance with another object for equality.
     * <p>
     * Two WhatsappSixPartsKeys instances are considered equal if they have the same phone number,
     * noise key pair, identity key pair, and identity ID.
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof WhatsappSixPartsKeys that
                && Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(noiseKeyPair, that.noiseKeyPair) &&
                Objects.equals(identityKeyPair, that.identityKeyPair) &&
                Objects.deepEquals(identityId, that.identityId);
    }

    /**
     * Returns a hash code value for this WhatsappSixPartsKeys instance.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(phoneNumber, noiseKeyPair, identityKeyPair, Arrays.hashCode(identityId));
    }
}