package com.github.auties00.cobalt.calls.transport.stun;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.CRC32;
import com.github.auties00.cobalt.calls.transport.warp.WarpCodecSupport;

/**
 * Computes and verifies the two integrity attributes a finalized STUN message carries, the
 * {@code MESSAGE-INTEGRITY} HMAC SHA1 keyed by the ICE password and the {@code FINGERPRINT} CRC32, both
 * over the length adjusted message prefix STUN defines.
 *
 * <p>STUN's two integrity attributes share one quirk: each is computed over the message header and all
 * preceding attributes, but with the header's sixteen bit length field temporarily rewritten to the
 * length the message will have once that attribute is appended. The {@code MESSAGE-INTEGRITY} HMAC
 * (RFC 8489 section 14.6) is taken over the header and every attribute up to but not including itself,
 * with the length field set to span through the twenty four byte integrity attribute; the
 * {@code FINGERPRINT} CRC32 (RFC 8489 section 14.7) is taken over the header and every attribute up to
 * but not including itself, with the length field set to span through the eight byte fingerprint
 * attribute, and the CRC32 is then XORed with the fixed value {@code 0x5354554E}. Both
 * helpers here take the message prefix exactly as it stands before the new attribute is appended and
 * apply the length rewrite internally, so a caller appends attributes, calls
 * {@link #computeMessageIntegrity(byte[], byte[])}, appends the resulting twenty bytes as a
 * {@link StunAttributeType#MESSAGE_INTEGRITY} attribute, calls {@link #computeFingerprint(byte[])},
 * and appends the resulting four bytes as a {@link StunAttributeType#FINGERPRINT} attribute, in that
 * order.
 *
 * <p>The length rewrite operates on a copy: neither helper mutates the caller's prefix array. The
 * verification helpers reverse the construction, recomputing the expected tag over the same length
 * adjusted prefix and comparing it against the value carried in the message, the HMAC under a constant
 * time comparison.
 *
 * @implNote This implementation computes HMAC SHA1 through {@code Mac("HmacSHA1")} and CRC32 through
 * {@link CRC32}, both exact standard algorithms (RFC 2104 HMAC over SHA-1, ISO 3309
 * CRC32), rather than a native binding. The {@code 0x5354554E} constant is the ASCII {@code "STUN"} the
 * RFC fixes. Applying the length rewrite to a copy leaves the caller's prefix buffer untouched.
 */
public final class StunIntegrity {
    /**
     * The logger for {@link StunIntegrity}.
     */
    private static final System.Logger LOGGER = Log.get(StunIntegrity.class);

    /**
     * Holds the JCA algorithm name of the {@code MESSAGE-INTEGRITY} primitive.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    /**
     * Holds the length, in bytes, of the {@code MESSAGE-INTEGRITY} HMAC SHA1 tag.
     */
    public static final int MESSAGE_INTEGRITY_LENGTH = 20;

    /**
     * Holds the length, in bytes, of the {@code FINGERPRINT} CRC32 value.
     */
    public static final int FINGERPRINT_LENGTH = 4;

    /**
     * Holds the constant the {@code FINGERPRINT} CRC32 is XORed with, the ASCII bytes of {@code "STUN"}.
     */
    public static final long FINGERPRINT_XOR = 0x5354554EL;

    /**
     * Holds the byte offset of the STUN header's sixteen bit message length field.
     */
    private static final int LENGTH_FIELD_OFFSET = 2;

    /**
     * Holds the size, in bytes, of the fixed STUN message header that precedes the attribute section.
     */
    private static final int HEADER_LENGTH = 20;

    /**
     * Holds the total on wire size, in bytes, of a {@code MESSAGE-INTEGRITY} attribute: a four byte
     * type length header plus the twenty byte HMAC value.
     */
    private static final int MESSAGE_INTEGRITY_ATTR_SIZE = 4 + MESSAGE_INTEGRITY_LENGTH;

    /**
     * Holds the total on wire size, in bytes, of a {@code FINGERPRINT} attribute: a four byte
     * type length header plus the four byte CRC32 value.
     */
    private static final int FINGERPRINT_ATTR_SIZE = 4 + FINGERPRINT_LENGTH;

    /**
     * Holds the per thread HMAC SHA1 engine reused across {@code MESSAGE-INTEGRITY} computations.
     *
     * <p>The integrity helpers run on transport threads; the mutable JCA engine is thread confined so a
     * concurrent computation on another transport thread cannot corrupt it. The ICE password varies per
     * call, so only the engine is reused and it is rekeyed on each computation.
     */
    private static final ThreadLocal<Mac> HMAC = ThreadLocal.withInitial(() -> {
        try {
            return Mac.getInstance(HMAC_ALGORITHM);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "cannot create stun message-integrity hmac-sha1 engine", e);
            }
            throw new WhatsAppCallException.Srtp("Cannot create STUN MESSAGE-INTEGRITY HMAC-SHA1", e);
        }
    });

    /**
     * Prevents instantiation of this stateless integrity primitive holder.
     */
    private StunIntegrity() {
        throw new AssertionError("StunIntegrity is not instantiable");
    }

    /**
     * Computes the {@code MESSAGE-INTEGRITY} HMAC SHA1 over a STUN message prefix.
     *
     * <p>The {@code prefix} is the STUN header followed by every attribute that precedes the integrity
     * attribute, exactly as it stands before the integrity attribute is appended. This helper copies
     * the prefix, rewrites the copy's length field to the value the message will carry once the
     * twenty four byte integrity attribute is appended, HMAC SHA1s the copy under {@code password}, and
     * returns the twenty byte tag the caller writes as the integrity attribute value.
     *
     * @param prefix   the STUN header and preceding attributes, at least {@value #HEADER_LENGTH} bytes
     * @param password the ICE password keying the HMAC, in raw bytes
     * @return the HMAC SHA1 tag of {@value #MESSAGE_INTEGRITY_LENGTH} bytes
     * @throws NullPointerException       if {@code prefix} or {@code password} is {@code null}
     * @throws IllegalArgumentException   if {@code prefix} is shorter than the STUN header
     * @throws WhatsAppCallException.Srtp if the platform cannot compute HMAC SHA1
     */
    public static byte[] computeMessageIntegrity(byte[] prefix, byte[] password) {
        Objects.requireNonNull(prefix, "prefix cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        var adjusted = withLengthThrough(prefix, MESSAGE_INTEGRITY_ATTR_SIZE);
        try {
            var mac = HMAC.get();
            mac.init(new SecretKeySpec(password, HMAC_ALGORITHM));
            return mac.doFinal(adjusted);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "stun message-integrity computation failed", e);
            }
            throw new WhatsAppCallException.Srtp("Cannot compute STUN MESSAGE-INTEGRITY", e);
        }
    }

    /**
     * Computes the {@code FINGERPRINT} value over a STUN message prefix.
     *
     * <p>The {@code prefix} is the STUN header followed by every attribute that precedes the fingerprint
     * attribute, including the {@code MESSAGE-INTEGRITY} attribute when present, exactly as it stands
     * before the fingerprint attribute is appended. This helper copies the prefix, rewrites the copy's
     * length field to the value the message will carry once the eight byte fingerprint attribute is
     * appended, CRC32s the copy, XORs the checksum with {@code 0x5354554E}, and returns the
     * four byte big endian result the caller writes as the fingerprint attribute value.
     *
     * @param prefix the STUN header and preceding attributes, at least {@value #HEADER_LENGTH} bytes
     * @return the fingerprint value of {@value #FINGERPRINT_LENGTH} bytes, big endian
     * @throws NullPointerException     if {@code prefix} is {@code null}
     * @throws IllegalArgumentException if {@code prefix} is shorter than the STUN header
     */
    public static byte[] computeFingerprint(byte[] prefix) {
        Objects.requireNonNull(prefix, "prefix cannot be null");
        var adjusted = withLengthThrough(prefix, FINGERPRINT_ATTR_SIZE);
        var crc = new CRC32();
        crc.update(adjusted);
        var value = (crc.getValue() ^ FINGERPRINT_XOR) & 0xFFFFFFFFL;
        var out = new byte[4];
        WarpCodecSupport.putU32(out, 0, (int) value);
        return out;
    }

    /**
     * Computes the {@code MESSAGE-INTEGRITY} HMAC SHA1 over the leading bytes of an in place STUN buffer.
     *
     * <p>Unlike {@link #computeMessageIntegrity(byte[], byte[])}, this overload neither copies the buffer
     * nor rewrites its length field: it HMAC SHA1s {@code buffer[0..prefixLen)} exactly as it stands, so the
     * caller must have already written the header length field to the value the message carries once the
     * twenty four byte integrity attribute is appended. It exists for the single buffer
     * {@link StunMessage#finalizeWithIntegrity(byte[])} path, which sizes one buffer and writes the header,
     * attributes, and both integrity attributes in place rather than appending grown copies.
     *
     * @param buffer    the STUN buffer whose header and preceding attributes occupy {@code buffer[0..prefixLen)}
     * @param prefixLen the number of leading bytes the HMAC covers, the header plus preceding attributes
     * @param password  the ICE password keying the HMAC, in raw bytes
     * @return the HMAC SHA1 tag of {@value #MESSAGE_INTEGRITY_LENGTH} bytes
     * @throws NullPointerException       if {@code buffer} or {@code password} is {@code null}
     * @throws IndexOutOfBoundsException  if {@code prefixLen} is negative or runs past {@code buffer}
     * @throws WhatsAppCallException.Srtp if the platform cannot compute HMAC SHA1
     */
    public static byte[] computeMessageIntegrity(byte[] buffer, int prefixLen, byte[] password) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        Objects.checkFromIndexSize(0, prefixLen, buffer.length);
        try {
            var mac = HMAC.get();
            mac.init(new SecretKeySpec(password, HMAC_ALGORITHM));
            mac.update(buffer, 0, prefixLen);
            return mac.doFinal();
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "stun message-integrity computation failed", e);
            }
            throw new WhatsAppCallException.Srtp("Cannot compute STUN MESSAGE-INTEGRITY", e);
        }
    }

    /**
     * Computes the {@code FINGERPRINT} value over the leading bytes of an in place STUN buffer.
     *
     * <p>Unlike {@link #computeFingerprint(byte[])}, this overload neither copies the buffer nor rewrites
     * its length field: it CRC32s {@code buffer[0..prefixLen)} exactly as it stands and XORs the checksum
     * with {@code 0x5354554E}, so the caller must have already written the header length field to the value
     * the message carries once the eight byte fingerprint attribute is appended. It serves the single buffer
     * {@link StunMessage#finalizeWithIntegrity(byte[])} path.
     *
     * @param buffer    the STUN buffer whose header and preceding attributes occupy {@code buffer[0..prefixLen)}
     * @param prefixLen the number of leading bytes the CRC covers, the header plus preceding attributes
     * @return the fingerprint value of {@value #FINGERPRINT_LENGTH} bytes, big endian
     * @throws NullPointerException       if {@code buffer} is {@code null}
     * @throws IndexOutOfBoundsException  if {@code prefixLen} is negative or runs past {@code buffer}
     */
    public static byte[] computeFingerprint(byte[] buffer, int prefixLen) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        Objects.checkFromIndexSize(0, prefixLen, buffer.length);
        var crc = new CRC32();
        crc.update(buffer, 0, prefixLen);
        var value = (crc.getValue() ^ FINGERPRINT_XOR) & 0xFFFFFFFFL;
        var out = new byte[4];
        WarpCodecSupport.putU32(out, 0, (int) value);
        return out;
    }

    /**
     * Verifies that a STUN message's {@code MESSAGE-INTEGRITY} attribute matches the expected HMAC SHA1
     * under the given password.
     *
     * <p>The {@code message} is the full STUN message; {@code integrityOffset} is the byte offset of the
     * {@code MESSAGE-INTEGRITY} attribute's type length header. The expected tag is recomputed over the
     * message prefix up to {@code integrityOffset} with the length field rewritten to span through the
     * integrity attribute, exactly as {@link #computeMessageIntegrity(byte[], byte[])} does, then
     * compared in constant time against the twenty bytes carried in the message.
     *
     * @param message         the full STUN message bytes
     * @param integrityOffset the byte offset of the {@code MESSAGE-INTEGRITY} attribute header
     * @param password        the ICE password keying the HMAC, in raw bytes
     * @return {@code true} if the carried tag equals the recomputed tag, {@code false} otherwise
     * @throws NullPointerException       if {@code message} or {@code password} is {@code null}
     * @throws IllegalArgumentException   if {@code integrityOffset} does not leave room for the prefix
     *                                    and the twenty four byte integrity attribute
     * @throws WhatsAppCallException.Srtp if the platform cannot compute HMAC SHA1
     */
    public static boolean verifyMessageIntegrity(byte[] message, int integrityOffset, byte[] password) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        if (integrityOffset < HEADER_LENGTH
                || integrityOffset + MESSAGE_INTEGRITY_ATTR_SIZE > message.length) {
            throw new IllegalArgumentException("integrityOffset " + integrityOffset
                    + " does not fit a MESSAGE-INTEGRITY attribute in a " + message.length + "-byte message");
        }
        var prefix = Arrays.copyOf(message, integrityOffset);
        var expected = computeMessageIntegrity(prefix, password);
        var actual = Arrays.copyOfRange(
                message, integrityOffset + 4, integrityOffset + 4 + MESSAGE_INTEGRITY_LENGTH);
        var matches = MessageDigest.isEqual(expected, actual);
        if (!matches && Log.WARNING) {
            LOGGER.log(Level.WARNING, "stun message-integrity mismatch at offset {0}", integrityOffset);
        }
        return matches;
    }

    /**
     * Verifies that a STUN message's trailing {@code FINGERPRINT} attribute matches the expected CRC32.
     *
     * <p>The {@code message} is the full STUN message ending in an eight byte {@code FINGERPRINT}
     * attribute. The expected value is recomputed over the message prefix up to that attribute with the
     * length field rewritten to span through it, exactly as {@link #computeFingerprint(byte[])} does,
     * then compared against the four bytes carried in the message.
     *
     * @param message the full STUN message bytes, ending in a {@code FINGERPRINT} attribute
     * @return {@code true} if the carried value equals the recomputed value, {@code false} otherwise
     * @throws NullPointerException     if {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code message} is shorter than a header plus a
     *                                  {@code FINGERPRINT} attribute
     */
    public static boolean verifyFingerprint(byte[] message) {
        Objects.requireNonNull(message, "message cannot be null");
        if (message.length < HEADER_LENGTH + FINGERPRINT_ATTR_SIZE) {
            throw new IllegalArgumentException(
                    "message of " + message.length + " bytes is too short to hold a FINGERPRINT attribute");
        }
        var fingerprintOffset = message.length - FINGERPRINT_ATTR_SIZE;
        var prefix = Arrays.copyOf(message, fingerprintOffset);
        var expected = computeFingerprint(prefix);
        var actual = Arrays.copyOfRange(message, fingerprintOffset + 4, message.length);
        var matches = Arrays.equals(expected, actual);
        if (!matches && Log.WARNING) {
            LOGGER.log(Level.WARNING, "stun fingerprint mismatch");
        }
        return matches;
    }

    /**
     * Returns a copy of a STUN message prefix whose header length field is rewritten to span the prefix
     * attributes plus one trailing attribute of the given size.
     *
     * <p>STUN's integrity computations require the header length to reflect the message length through
     * the attribute being computed, even though that attribute is not yet present in the bytes being
     * hashed. The returned copy carries the attribute section length, that is the prefix length beyond
     * the twenty byte header plus {@code trailingAttrSize}, in its sixteen bit big endian length field.
     *
     * @param prefix           the STUN header and preceding attributes
     * @param trailingAttrSize the on wire size of the attribute about to be appended
     * @return a copy of {@code prefix} with the length field adjusted
     * @throws IllegalArgumentException if {@code prefix} is shorter than the STUN header
     */
    private static byte[] withLengthThrough(byte[] prefix, int trailingAttrSize) {
        if (prefix.length < HEADER_LENGTH) {
            throw new IllegalArgumentException(
                    "STUN prefix of " + prefix.length + " bytes is shorter than the " + HEADER_LENGTH
                            + "-byte header");
        }
        var copy = Arrays.copyOf(prefix, prefix.length);
        var attributeSectionLength = prefix.length - HEADER_LENGTH + trailingAttrSize;
        WarpCodecSupport.putU16(copy, LENGTH_FIELD_OFFSET, attributeSectionLength);
        return copy;
    }
}
