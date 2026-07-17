package com.github.auties00.cobalt.calls.media.sframe;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Derives the per participant 32 byte SFrame base (chain) key that seeds the end to end media
 * encryption shared between the clients of a call.
 *
 * <p>SFrame is the inner, end to end layer of the WhatsApp call media path: the audio and video
 * payloads are encrypted with SFrame by the sender, the relay or SFU forwards the ciphertext without
 * being able to read it, and the receiver decrypts it. Each connected participant device owns one
 * SFrame chain key, derived here from the 32 byte end to end call key (the shared secret transported
 * in the encrypted call offer or rotated through an {@code enc_rekey}); the
 * {@link SFrameKeyProvider} then installs that chain key and resolves the per frame cipher from it.
 *
 * <p>The base key is a single {@code HKDF-SHA256} output. The 32 byte call key is split into two
 * 16 byte halves: the first half is the HKDF salt and the second half is the HKDF input keying
 * material. The info parameter is the fixed ASCII label {@value #INFO_LABEL_STRING} immediately
 * followed, with no separator and no trailing {@code NUL}, by the participant's device JID in raw
 * ASCII:
 *
 * {@snippet :
 *   salt = callKey[0 .. 16]                      // first 16 bytes
 *   ikm  = callKey[16 .. 32]                     // second 16 bytes
 *   info = "e2e sframe key" + deviceJid          // ASCII, no NUL, no separator
 *   baseKey = HKDF-SHA256(ikm, salt, info, L = 32)
 * }
 *
 * <p>Because the only input that varies between participants is the device JID embedded in the info,
 * each participant device derives a distinct base key from the same call key. A device JID has the
 * form {@code <user>:<device>@lid}, for example {@code "83116928594056:2@lid"}.
 *
 * @implNote This implementation splits the 32 byte call key as {@code salt = callKey[0..16]} and
 * {@code IKM = callKey[16..32]}, builds a {@value #INFO_LABEL_LENGTH} byte label immediately followed
 * by the device JID (truncated to {@value #MAX_CONTEXT_LENGTH} bytes, with no separator and no
 * trailing {@code NUL}), and requests a {@value #BASE_KEY_LENGTH} byte HKDF output. The HKDF itself
 * is delegated to {@link VoipCryptoNative#hkdfSha256(byte[], byte[], byte[], int)} (the JCA
 * {@code KDF("HKDF-SHA256")}). As a worked example, the call key
 * {@code 86e0004078464597d59c751fde9a8b61908dcbd04197ffdc7636582be7f439aa} with participant
 * {@code "83116928594056:2@lid"} derives the base key
 * {@code 409102bf2c1a3816c76a6d64819d0c901556e030d5f33da251c13cdfcf0b9353}.
 */
public final class SFrameKeyDerivation {
    /**
     * The logger for {@link SFrameKeyDerivation}.
     */
    private static final System.Logger LOGGER = Log.get(SFrameKeyDerivation.class);

    /**
     * Holds the required length, in bytes, of the call key used as the HKDF input.
     *
     * <p>The call key must be exactly this many bytes; the rekey path rejects any end to end key
     * longer than {@code 0x20} before derivation begins.
     */
    public static final int CALL_KEY_LENGTH = 32;

    /**
     * Holds the length, in bytes, of the derived SFrame base (chain) key.
     */
    public static final int BASE_KEY_LENGTH = 32;

    /**
     * Holds the maximum length, in bytes, of the participant context appended to the HKDF info.
     *
     * <p>The participant JID is bounded to {@code 0x50} (80) bytes; a longer JID is truncated to this
     * length before it enters the info.
     */
    public static final int MAX_CONTEXT_LENGTH = 0x50;

    /**
     * Holds the fixed ASCII info label as a string, referenced by {@code @value} in the surrounding
     * javadoc.
     */
    private static final String INFO_LABEL_STRING = "e2e sframe key";

    /**
     * Holds the length, in bytes, of the fixed ASCII info label.
     */
    private static final int INFO_LABEL_LENGTH = 14;

    /**
     * Holds the fixed ASCII label that prefixes the participant JID in the HKDF info, with no
     * separator and no trailing {@code NUL} before the JID.
     */
    private static final byte[] INFO_LABEL = INFO_LABEL_STRING.getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the length, in bytes, of each half of the call key; the first half is the HKDF salt and
     * the second half is the HKDF input keying material.
     */
    private static final int HALF = CALL_KEY_LENGTH / 2;

    /**
     * Prevents instantiation of this stateless derivation holder.
     */
    private SFrameKeyDerivation() {
        throw new AssertionError("SFrameKeyDerivation is not instantiable");
    }

    /**
     * Derives the SFrame base (chain) key for a single call participant device.
     *
     * <p>The {@code deviceJid} is the participant's device JID exactly as it appears on the signaling
     * layer, for example {@code "83116928594056:2@lid"}; its ASCII bytes, truncated to
     * {@value #MAX_CONTEXT_LENGTH} bytes, are appended to the fixed label to form the HKDF info. The
     * call key is split with the first 16 bytes as the salt and the second 16 bytes as the input
     * keying material.
     *
     * @param callKey   the 32 byte end to end call key shared by the call participants
     * @param deviceJid the device JID whose base key is derived
     * @return the 32-byte SFrame base key for {@code deviceJid}
     * @throws NullPointerException     if {@code callKey} or {@code deviceJid} is {@code null}
     * @throws IllegalArgumentException if {@code callKey} is not exactly {@value #CALL_KEY_LENGTH}
     *                                  bytes long
     */
    public static byte[] deriveBaseKey(byte[] callKey, String deviceJid) {
        Objects.requireNonNull(callKey, "callKey cannot be null");
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        if (callKey.length != CALL_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "callKey must be " + CALL_KEY_LENGTH + " bytes, got " + callKey.length);
        }
        var salt = Arrays.copyOfRange(callKey, 0, HALF);
        var ikm = Arrays.copyOfRange(callKey, HALF, CALL_KEY_LENGTH);
        var info = buildInfo(deviceJid);
        var baseKey = VoipCryptoNative.hkdfSha256(ikm, salt, info, BASE_KEY_LENGTH);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "sframe base key derived for {0}", new LogRedactable.User(deviceJid));
        return baseKey;
    }

    /**
     * Builds the HKDF info buffer for a participant device JID.
     *
     * <p>The buffer is the fixed {@value #INFO_LABEL_STRING} label immediately followed by the device
     * JID's ASCII bytes, truncated to {@value #MAX_CONTEXT_LENGTH} bytes, with no separator and no
     * trailing {@code NUL}.
     *
     * @param deviceJid the participant device JID
     * @return the HKDF info bytes
     */
    private static byte[] buildInfo(String deviceJid) {
        var jidBytes = deviceJid.getBytes(StandardCharsets.US_ASCII);
        var contextLength = Math.min(jidBytes.length, MAX_CONTEXT_LENGTH);
        var info = new byte[INFO_LABEL_LENGTH + contextLength];
        System.arraycopy(INFO_LABEL, 0, info, 0, INFO_LABEL_LENGTH);
        System.arraycopy(jidBytes, 0, info, INFO_LABEL_LENGTH, contextLength);
        return info;
    }
}
