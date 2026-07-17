
package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPrivateKey;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * A portable representation of a WhatsApp mobile account's long-lived
 * credentials, encoded as six comma-separated fields.
 *
 * @apiNote
 * Used to import or export the minimal set of credentials needed to
 * resume a mobile session across deployments. The format is the
 * de-facto interchange shape used by the WhatsApp reverse-engineering
 * community: phone number plus Noise key pair (server handshake) plus
 * Signal identity key pair (end-to-end encryption) plus identity
 * identifier. Cobalt loads a store from these via
 * {@link LinkedWhatsAppClientBuilder.Client.Mobile#createConnection(LinkedWhatsAppClientSixPartsKeys)}
 * and emits them back via {@link #toString()}. The serialised shape
 * reads as
 * {@snippet :
 *     <phone>,<noisePub>,<noisePriv>,<sigPub>,<sigPriv>,<identityId>
 * }
 * where every cryptographic field is standard Base64 and the phone
 * number is decimal with an optional {@code +} prefix.
 *
 * @see LinkedWhatsAppClientBuilder
 * @see SignalIdentityKeyPair
 */
public final class LinkedWhatsAppClientSixPartsKeys {
    /**
     * The logger for {@link LinkedWhatsAppClientSixPartsKeys}.
     */
    private static final System.Logger LOGGER = Log.get(LinkedWhatsAppClientSixPartsKeys.class);

    /**
     * The account phone number stored as an unsigned long without the
     * leading {@code +}.
     */
    private final long phoneNumber;

    /**
     * The Noise protocol key pair used during the WhatsApp server
     * handshake.
     */
    private final SignalIdentityKeyPair noiseKeyPair;

    /**
     * The Signal identity key pair used for end-to-end encrypted
     * messaging.
     */
    private final SignalIdentityKeyPair identityKeyPair;

    /**
     * The opaque identity identifier issued by WhatsApp during account
     * registration.
     */
    private final byte[] identityId;

    /**
     * Constructs a new credentials record from its four logical
     * components.
     *
     * @apiNote
     * Most callers should prefer {@link #of(String)} to parse a
     * serialised six-parts string. This constructor is the seam for
     * applications that already hold the keys in object form.
     *
     * @param phoneNumber     the account phone number, stored as an
     *                        unsigned long
     * @param noiseKeyPair    the Noise protocol key pair
     * @param identityKeyPair the Signal identity key pair
     * @param identityId      the opaque identity identifier
     */
    public LinkedWhatsAppClientSixPartsKeys(long phoneNumber, SignalIdentityKeyPair noiseKeyPair, SignalIdentityKeyPair identityKeyPair, byte[] identityId) {
        this.phoneNumber = phoneNumber;
        this.noiseKeyPair = noiseKeyPair;
        this.identityKeyPair = identityKeyPair;
        this.identityId = identityId;
    }

    /**
     * Parses a comma-separated six-parts credentials string into a
     * {@link LinkedWhatsAppClientSixPartsKeys}.
     *
     * @apiNote
     * Use this to import credentials minted elsewhere (typically by
     * another Cobalt deployment or by a third-party WhatsApp
     * reverse-engineering tool). The parser tolerates leading and
     * trailing whitespace and strips embedded newlines so multi-line
     * pasted input works without preprocessing.
     *
     * @implNote
     * This implementation splits on the first five commas only ({@code split(",", 6)})
     * so a Base64 trailing equals does not get truncated. The phone
     * number field accepts an optional {@code +} prefix and is parsed
     * as an unsigned decimal {@code long}.
     *
     * @param sixParts the comma-separated credentials string
     * @return the parsed credentials record
     * @throws NullPointerException     if {@code sixParts} is {@code null}
     * @throws IllegalArgumentException if the input does not decode to
     *                                  exactly six parts or the phone
     *                                  number is malformed
     */
    public static LinkedWhatsAppClientSixPartsKeys of(String sixParts) {
        Objects.requireNonNull(sixParts, "Invalid six parts");
        var parts = sixParts.trim()
                .replaceAll("\n", "")
                .split(",", 6);
        if (parts.length != 6) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed six-parts credentials, expected 6 fields got {0}", parts.length);
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
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsed six-parts credentials for {0}", new LogRedactable.Phone(phoneNumber));
        return new LinkedWhatsAppClientSixPartsKeys(phoneNumber, noiseKeyPair, identityKeyPair, identityId);
    }

    /**
     * Parses the phone number from the first element of the split
     * six-parts array.
     *
     * @implNote
     * This implementation strips a leading {@code +} when present and
     * then defers to {@link Long#parseUnsignedLong(CharSequence, int, int, int)}
     * over the remainder so country-code prefixed numbers are accepted
     * without allocating a substring.
     *
     * @param parts the already-split six-parts array
     * @return the phone number as an unsigned {@code long}
     * @throws IllegalArgumentException if the phone number field is
     *                                  empty or does not parse as an
     *                                  unsigned decimal integer
     */
    private static long parsePhoneNumber(String[] parts) {
        var rawPhoneNumber = parts[0];
        if(rawPhoneNumber.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "six-parts credentials: empty phone number field");
            throw new IllegalArgumentException("Invalid phone number: " + rawPhoneNumber);
        }
        try {
            return Long.parseUnsignedLong(rawPhoneNumber, rawPhoneNumber.charAt(0) == '+' ? 1 : 0, rawPhoneNumber.length(), 10);
        }catch (NumberFormatException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "six-parts credentials: malformed phone number {0}", new LogRedactable.Phone(rawPhoneNumber));
            throw new IllegalArgumentException("Invalid phone number: " + rawPhoneNumber);
        }
    }

    /**
     * Serialises this credentials record to its canonical six-parts
     * string form.
     *
     * @apiNote
     * The returned string is round-trippable through {@link #of(String)}
     * and is the canonical export shape every WhatsApp
     * reverse-engineering tool understands. Cryptographic fields use the
     * standard Base64 alphabet with padding preserved.
     *
     * @return the six-parts string representation
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
     * @return the phone number as an unsigned {@code long}
     */
    public long phoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns the Noise protocol key pair used to establish the
     * encrypted transport with the WhatsApp servers.
     *
     * @return the Noise handshake key pair
     */
    public SignalIdentityKeyPair noiseKeyPair() {
        return noiseKeyPair;
    }

    /**
     * Returns the Signal identity key pair used for end-to-end
     * encrypted messaging.
     *
     * @apiNote
     * The returned pair is the long-term key material that identifies
     * the account across Signal sessions and feeds into the WhatsApp
     * Double Ratchet derivation.
     *
     * @return the Signal identity key pair
     */
    public SignalIdentityKeyPair identityKeyPair() {
        return identityKeyPair;
    }

    /**
     * Returns the opaque identity identifier bound to this account.
     *
     * @apiNote
     * The returned array is the internal backing storage; callers must
     * not mutate it.
     *
     * @return the identity ID
     */
    public byte[] identityId() {
        return identityId;
    }

    /**
     * Compares this credentials record to another object for structural
     * equality.
     *
     * <p>Two records are equal when their phone number, Noise key pair,
     * Signal identity key pair, and identity ID all match.
     *
     * @param o the object to compare with
     * @return {@code true} if the records are equal
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof LinkedWhatsAppClientSixPartsKeys that
                && Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(noiseKeyPair, that.noiseKeyPair) &&
                Objects.equals(identityKeyPair, that.identityKeyPair) &&
                Objects.deepEquals(identityId, that.identityId);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(phoneNumber, noiseKeyPair, identityKeyPair, Arrays.hashCode(identityId));
    }
}
