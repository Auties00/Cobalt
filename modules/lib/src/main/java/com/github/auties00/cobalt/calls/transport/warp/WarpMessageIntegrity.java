package com.github.auties00.cobalt.calls.transport.warp;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Objects;
import com.github.auties00.cobalt.calls.transport.LiveRelayTransport;

/**
 * Computes and appends the hop by hop message integrity tag a WARP control message carries so the relay
 * or SFU can authenticate it without holding the call's end to end keys.
 *
 * <p>A {@link WarpMessage} travels toward the relay either piggybacked on an RTP packet or standalone.
 * When hop by hop integrity is enabled, the sender appends an {@code HMAC-SHA256} tag computed over the
 * encoded WARP bytes keyed by the per relay {@value #WARP_AUTH_KEY_LABEL} key, and the relay verifies that
 * tag with the same key before acting on the control message. The tag is a suffix: it is appended after
 * the complete WARP message, so the bytes authenticated are exactly the {@link WarpMessage#encode()}
 * output, and the transmitted packet is the WARP message followed by the tag.
 *
 * <p>The {@value #WARP_AUTH_KEY_LABEL} key is one output of the call's SFU key derivation schedule, a two
 * step chained {@code HKDF-SHA256} over the relay hop by hop key using the {@code "warp auth salt"} step
 * then the {@value #WARP_AUTH_KEY_LABEL} step, derived by
 * {@link com.github.auties00.cobalt.calls.crypto.CallE2eKeyDerivation#deriveWarpAuthKey(byte[])}. Deriving
 * and caching that key per relay is the relay transport's responsibility; this holder only consumes the
 * already derived key.
 *
 * <p>The tag length the relay expects is a per relay parameter carried by the {@code warp_mi_tag_len}
 * attribute of the {@code <relay>} signaling block, and the media bring up passes that length and the
 * derived {@value #WARP_AUTH_KEY_LABEL} key to {@link LiveRelayTransport}, so integrity is enabled exactly
 * when and to the length the relay advertises. When the relay omits the attribute, integrity is off. The
 * full {@code HMAC-SHA256} output is {@value #FULL_TAG_LENGTH} bytes: {@link #appendTag(byte[], byte[])}
 * appends the full tag, while {@link #appendTag(byte[], byte[], int)} appends a leading byte truncation
 * when a relay negotiates a shorter tag.
 *
 * @implNote This implementation computes the tag through
 * {@link VoipCryptoNative#hmacSha256(byte[], byte[])} (the JCA {@code Mac("HmacSHA256")}) rather than a
 * native binding. The full {@value #FULL_TAG_LENGTH}-byte output is the default, and
 * {@link #appendTag(byte[], byte[], int)} carries the per relay truncation as a caller supplied parameter.
 */
public final class WarpMessageIntegrity {
    /**
     * The logger for {@link WarpMessageIntegrity}.
     */
    private static final System.Logger LOGGER = Log.get(WarpMessageIntegrity.class);

    /**
     * Holds the HKDF info label naming the hop by hop WARP authentication key in the SFU key schedule.
     */
    public static final String WARP_AUTH_KEY_LABEL = "warp auth key";

    /**
     * Holds the length, in bytes, of the full {@code HMAC-SHA256} tag, the default hop by hop WARP
     * integrity tag length.
     */
    public static final int FULL_TAG_LENGTH = 32;

    /**
     * Prevents instantiation of this stateless integrity primitive holder.
     */
    private WarpMessageIntegrity() {
        throw new AssertionError("WarpMessageIntegrity is not instantiable");
    }

    /**
     * Computes the full {@code HMAC-SHA256} hop by hop integrity tag over a WARP message.
     *
     * <p>The {@code warpBytes} are the encoded WARP message exactly as it will be transmitted before the
     * tag is appended, that is the {@link WarpMessage#encode()} output. The tag is keyed by the
     * {@value #WARP_AUTH_KEY_LABEL} key.
     *
     * @param warpBytes   the encoded WARP message bytes the tag authenticates
     * @param warpAuthKey the derived {@value #WARP_AUTH_KEY_LABEL} key, in raw bytes
     * @return the {@value #FULL_TAG_LENGTH}-byte {@code HMAC-SHA256} tag
     * @throws NullPointerException       if {@code warpBytes} or {@code warpAuthKey} is {@code null}
     * @throws WhatsAppCallException.Srtp if the platform cannot compute {@code HMAC-SHA256}
     */
    public static byte[] computeTag(byte[] warpBytes, byte[] warpAuthKey) {
        Objects.requireNonNull(warpBytes, "warpBytes cannot be null");
        Objects.requireNonNull(warpAuthKey, "warpAuthKey cannot be null");
        return VoipCryptoNative.hmacSha256(warpAuthKey, warpBytes);
    }

    /**
     * Appends the full {@code HMAC-SHA256} hop by hop integrity tag to an encoded WARP message.
     *
     * <p>The returned buffer is the WARP message followed by the {@value #FULL_TAG_LENGTH}-byte tag
     * computed over the message; the input array is not modified.
     *
     * @param warpBytes   the encoded WARP message bytes
     * @param warpAuthKey the derived {@value #WARP_AUTH_KEY_LABEL} key, in raw bytes
     * @return a new buffer holding the WARP message followed by the integrity tag
     * @throws NullPointerException       if {@code warpBytes} or {@code warpAuthKey} is {@code null}
     * @throws WhatsAppCallException.Srtp if the platform cannot compute {@code HMAC-SHA256}
     */
    public static byte[] appendTag(byte[] warpBytes, byte[] warpAuthKey) {
        return appendTag(warpBytes, warpAuthKey, FULL_TAG_LENGTH);
    }

    /**
     * Appends a possibly truncated {@code HMAC-SHA256} hop by hop integrity tag to an encoded WARP message.
     *
     * <p>The tag is the leading {@code tagLength} bytes of the {@code HMAC-SHA256} of the WARP message; a
     * relay that negotiates a shorter tag length receives that many leading tag bytes. The returned buffer
     * is the WARP message followed by the truncated tag; the input array is not modified.
     *
     * @param warpBytes   the encoded WARP message bytes
     * @param warpAuthKey the derived {@value #WARP_AUTH_KEY_LABEL} key, in raw bytes
     * @param tagLength   the number of leading HMAC bytes to append, in {@code 1..}{@value #FULL_TAG_LENGTH}
     * @return a new buffer holding the WARP message followed by the truncated integrity tag
     * @throws NullPointerException       if {@code warpBytes} or {@code warpAuthKey} is {@code null}
     * @throws IllegalArgumentException   if {@code tagLength} is not in {@code 1..}{@value #FULL_TAG_LENGTH}
     * @throws WhatsAppCallException.Srtp if the platform cannot compute {@code HMAC-SHA256}
     */
    public static byte[] appendTag(byte[] warpBytes, byte[] warpAuthKey, int tagLength) {
        Objects.requireNonNull(warpBytes, "warpBytes cannot be null");
        Objects.requireNonNull(warpAuthKey, "warpAuthKey cannot be null");
        requireValidTagLength(tagLength);
        // TODO: no observed relay block advertises warp_mi_tag_len, so the full and truncated tags are not
        //  yet byte validated against a live integrity enabled relay datagram
        var tag = VoipCryptoNative.hmacSha256(warpAuthKey, warpBytes);
        var out = Arrays.copyOf(warpBytes, warpBytes.length + tagLength);
        System.arraycopy(tag, 0, out, warpBytes.length, tagLength);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "warp integrity tag appended len={0} totalBytes={1}", tagLength, out.length);
        }
        return out;
    }

    /**
     * Verifies that an inbound WARP packet's trailing hop by hop integrity tag matches the expected
     * {@code HMAC-SHA256} under the {@value #WARP_AUTH_KEY_LABEL} key.
     *
     * <p>The {@code taggedWarpBytes} are the WARP message followed by a {@code tagLength}-byte tag. The
     * expected tag is recomputed over the WARP message prefix (the bytes before the tag) and compared in
     * constant time against the carried tag.
     *
     * @param taggedWarpBytes the WARP message followed by its integrity tag
     * @param warpAuthKey     the derived {@value #WARP_AUTH_KEY_LABEL} key, in raw bytes
     * @param tagLength       the length, in bytes, of the trailing tag, in
     *                        {@code 1..}{@value #FULL_TAG_LENGTH}
     * @return {@code true} if the carried tag equals the leading {@code tagLength} bytes of the
     *         recomputed HMAC, {@code false} otherwise
     * @throws NullPointerException       if {@code taggedWarpBytes} or {@code warpAuthKey} is {@code null}
     * @throws IllegalArgumentException   if {@code tagLength} is not in {@code 1..}{@value #FULL_TAG_LENGTH}
     *                                    or exceeds the packet length
     * @throws WhatsAppCallException.Srtp if the platform cannot compute {@code HMAC-SHA256}
     */
    public static boolean verifyTag(byte[] taggedWarpBytes, byte[] warpAuthKey, int tagLength) {
        Objects.requireNonNull(taggedWarpBytes, "taggedWarpBytes cannot be null");
        Objects.requireNonNull(warpAuthKey, "warpAuthKey cannot be null");
        requireValidTagLength(tagLength);
        if (tagLength >= taggedWarpBytes.length) {
            throw new IllegalArgumentException("tagLength " + tagLength
                    + " leaves no WARP message in a " + taggedWarpBytes.length + "-byte packet");
        }
        var prefixLength = taggedWarpBytes.length - tagLength;
        var prefix = Arrays.copyOf(taggedWarpBytes, prefixLength);
        var expected = VoipCryptoNative.hmacSha256(warpAuthKey, prefix);
        var expectedTruncated = Arrays.copyOf(expected, tagLength);
        var actual = Arrays.copyOfRange(taggedWarpBytes, prefixLength, taggedWarpBytes.length);
        var matches = java.security.MessageDigest.isEqual(expectedTruncated, actual);
        if (Log.WARNING && !matches) {
            LOGGER.log(Level.WARNING, "warp integrity tag mismatch len={0}", tagLength);
        }
        return matches;
    }

    /**
     * Validates that a hop by hop WARP integrity tag length is within the {@code HMAC-SHA256} output bound.
     *
     * @param tagLength the requested tag length, in bytes
     * @throws IllegalArgumentException if {@code tagLength} is not in {@code 1..}{@value #FULL_TAG_LENGTH}
     */
    private static void requireValidTagLength(int tagLength) {
        if (tagLength < 1 || tagLength > FULL_TAG_LENGTH) {
            throw new IllegalArgumentException("WARP MI tag length must be in 1.." + FULL_TAG_LENGTH
                    + ", got " + tagLength);
        }
    }
}
