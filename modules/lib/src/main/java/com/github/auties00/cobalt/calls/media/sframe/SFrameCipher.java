package com.github.auties00.cobalt.calls.media.sframe;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/**
 * Seals and opens one SFrame media frame under a single key id, using {@code AES-128} in counter mode
 * for confidentiality and a truncated {@code HMAC-SHA256} over the trailer and ciphertext for
 * authentication.
 *
 * <p>A frame is sealed by deriving the per frame counter block from the frame counter, encrypting the
 * plaintext under it with {@code AES-CTR}, and computing the truncated {@code HMAC-SHA256} over the
 * SFrame trailer (the associated data) followed by the ciphertext. A frame is opened by recomputing and
 * verifying that tag in constant time before decrypting.
 *
 * <p>The 16 byte counter block for a frame is the frame counter serialized big endian into the middle
 * eight bytes of an otherwise zero 16 byte block (four zero bytes, the eight big endian counter bytes,
 * four zero bytes), encrypted under the AES key, then XORed with a stored counter mask. The truncated
 * tag is the leading {@code tagLength} bytes of {@code HMAC-SHA256(authKey, trailer || ciphertext)}; a
 * suite whose tag length is zero performs no authentication.
 *
 * <p>The counter mask is supplied by {@link SFrameKeyProvider} rather than computed here. It is a
 * 12 byte per key id salt copied into an otherwise zero 16 byte buffer; a call with no salt installed
 * uses the all zero mask returned by {@link #zeroCounterMask()}.
 *
 * <p>Each instance is confined to one stream direction and its media thread, so the mutable JCA engines
 * are reused fields rather than per frame {@code getInstance} calls.
 */
public final class SFrameCipher {
    /**
     * The logger for {@link SFrameCipher}.
     */
    private static final System.Logger LOGGER = Log.get(SFrameCipher.class);

    /**
     * Holds the JCA transformation name for AES in counter mode without padding.
     */
    private static final String AES_CTR_TRANSFORMATION = "AES/CTR/NoPadding";

    /**
     * Holds the JCA key algorithm name for the AES key.
     */
    private static final String AES_KEY_ALGORITHM = "AES";

    /**
     * Holds the JCA algorithm name for the {@code HMAC-SHA256} authentication primitive.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Holds the cipher suite selecting the cipher variant and the truncated tag length.
     */
    private final SFrameCipherSuite suite;

    /**
     * Holds the 16 byte {@code AES-128} encryption key.
     */
    private final byte[] aesKey;

    /**
     * Holds the 32 byte {@code HMAC-SHA256} authentication key.
     */
    private final byte[] authKey;

    /**
     * Holds the 16 byte counter mask XORed against each per frame AES encrypted counter block to form
     * the CTR initial block, supplied by {@link SFrameKeyProvider} from the per key id salt.
     */
    private final byte[] counterMask;

    /**
     * Holds the immutable AES key specification reused across frames on this cipher.
     */
    private final SecretKeySpec aesKeySpec;

    /**
     * Holds the immutable HMAC key specification reused across frames on this cipher.
     */
    private final SecretKeySpec macKeySpec;

    /**
     * Holds the AES counter mode engine reused across frames.
     *
     * <p>One {@link SFrameCipher} instance is confined to a single stream direction and its media thread,
     * so the mutable JCA engine is a reused field rather than a per frame {@code getInstance} call.
     */
    private final Cipher ctrCipher;

    /**
     * Holds the AES single block engine reused to turn each per frame counter block into keystream.
     */
    private final Cipher blockCipher;

    /**
     * Holds the {@code HMAC-SHA256} engine reused across frames for the truncated authentication tag.
     */
    private final Mac hmac;

    /**
     * Holds the 16 byte counter block scratch reused across frames.
     *
     * <p>Only the middle eight counter bytes are ever written; the surrounding bytes stay zero across
     * reuse, matching the fresh zero filled block the native layout expects.
     */
    private final byte[] counterScratch = new byte[SFrameCipherSuite.IV_LENGTH];

    /**
     * Holds the length, in bytes, of the per key id salt the counter mask is built from.
     *
     * <p>Exactly this many salt bytes are copied into the zero initialized 16 byte mask.
     */
    public static final int SALT_LENGTH = 12;

    /**
     * Constructs a cipher from the resolved per key id key material, the per key id counter mask, and
     * the negotiated suite.
     *
     * <p>The {@code aesKey} must be {@value SFrameCipherSuite#AES_KEY_LENGTH} bytes, the {@code authKey}
     * {@value SFrameCipherSuite#HMAC_KEY_LENGTH} bytes, and {@code counterMask}
     * {@value SFrameCipherSuite#IV_LENGTH} bytes. Build the mask from a 12 byte salt with
     * {@link #counterMaskFromSalt(byte[])}, or pass {@link #zeroCounterMask()} when no salt is installed.
     * The three arrays are defensively cloned, so the caller may retain and mutate them afterwards.
     *
     * @param suite       the cipher suite carrying the cipher variant and tag length
     * @param aesKey      the {@code AES-128} encryption key
     * @param authKey     the {@code HMAC-SHA256} authentication key
     * @param counterMask the 16 byte counter mask XORed into each counter block
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code aesKey}, {@code authKey}, or {@code counterMask} has the
     *                                  wrong length
     */
    public SFrameCipher(SFrameCipherSuite suite, byte[] aesKey, byte[] authKey, byte[] counterMask) {
        Objects.requireNonNull(suite, "suite cannot be null");
        Objects.requireNonNull(aesKey, "aesKey cannot be null");
        Objects.requireNonNull(authKey, "authKey cannot be null");
        Objects.requireNonNull(counterMask, "counterMask cannot be null");
        if (aesKey.length != SFrameCipherSuite.AES_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "aesKey must be " + SFrameCipherSuite.AES_KEY_LENGTH + " bytes, got " + aesKey.length);
        }
        if (authKey.length != SFrameCipherSuite.HMAC_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "authKey must be " + SFrameCipherSuite.HMAC_KEY_LENGTH + " bytes, got " + authKey.length);
        }
        if (counterMask.length != SFrameCipherSuite.IV_LENGTH) {
            throw new IllegalArgumentException(
                    "counterMask must be " + SFrameCipherSuite.IV_LENGTH + " bytes, got " + counterMask.length);
        }
        this.suite = suite;
        this.aesKey = aesKey.clone();
        this.authKey = authKey.clone();
        this.counterMask = counterMask.clone();
        this.aesKeySpec = new SecretKeySpec(this.aesKey, AES_KEY_ALGORITHM);
        this.macKeySpec = new SecretKeySpec(this.authKey, HMAC_ALGORITHM);
        try {
            this.ctrCipher = Cipher.getInstance(AES_CTR_TRANSFORMATION);
            this.blockCipher = Cipher.getInstance("AES/ECB/NoPadding");
            this.hmac = Mac.getInstance(HMAC_ALGORITHM);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sframe cipher init failed suite=" + suite, e);
            throw new WhatsAppCallException.Srtp("SFrame cipher initialization failed", e);
        }
    }

    /**
     * Constructs a cipher taking ownership of the key material and counter mask without cloning them.
     *
     * <p>The {@code owned} marker distinguishes this constructor from the public cloning constructor of
     * the same erasure. It stores the three arrays by reference, so the caller must have produced them
     * freshly and must neither retain nor mutate them after the handoff; the fields keep the passed
     * arrays rather than defensive copies. The length validation is identical to the public
     * constructor.
     *
     * @param suite       the cipher suite carrying the cipher variant and tag length
     * @param aesKey      the {@code AES-128} encryption key, ownership transferred
     * @param authKey     the {@code HMAC-SHA256} authentication key, ownership transferred
     * @param counterMask the 16 byte counter mask, ownership transferred
     * @param owned       a marker, always {@code true}, selecting the no clone ownership transfer path
     * @throws NullPointerException     if any array argument is {@code null}
     * @throws IllegalArgumentException if {@code aesKey}, {@code authKey}, or {@code counterMask} has
     *                                  the wrong length
     */
    private SFrameCipher(SFrameCipherSuite suite, byte[] aesKey, byte[] authKey, byte[] counterMask,
                         boolean owned) {
        Objects.requireNonNull(suite, "suite cannot be null");
        Objects.requireNonNull(aesKey, "aesKey cannot be null");
        Objects.requireNonNull(authKey, "authKey cannot be null");
        Objects.requireNonNull(counterMask, "counterMask cannot be null");
        if (aesKey.length != SFrameCipherSuite.AES_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "aesKey must be " + SFrameCipherSuite.AES_KEY_LENGTH + " bytes, got " + aesKey.length);
        }
        if (authKey.length != SFrameCipherSuite.HMAC_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "authKey must be " + SFrameCipherSuite.HMAC_KEY_LENGTH + " bytes, got " + authKey.length);
        }
        if (counterMask.length != SFrameCipherSuite.IV_LENGTH) {
            throw new IllegalArgumentException(
                    "counterMask must be " + SFrameCipherSuite.IV_LENGTH + " bytes, got " + counterMask.length);
        }
        this.suite = suite;
        this.aesKey = aesKey;
        this.authKey = authKey;
        this.counterMask = counterMask;
        this.aesKeySpec = new SecretKeySpec(this.aesKey, AES_KEY_ALGORITHM);
        this.macKeySpec = new SecretKeySpec(this.authKey, HMAC_ALGORITHM);
        try {
            this.ctrCipher = Cipher.getInstance(AES_CTR_TRANSFORMATION);
            this.blockCipher = Cipher.getInstance("AES/ECB/NoPadding");
            this.hmac = Mac.getInstance(HMAC_ALGORITHM);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sframe cipher init failed suite=" + suite, e);
            throw new WhatsAppCallException.Srtp("SFrame cipher initialization failed", e);
        }
    }

    /**
     * Constructs a cipher that takes ownership of freshly derived key material without cloning it.
     *
     * <p>This is the ownership transfer factory for callers that derive the AES key, HMAC key, and
     * counter mask solely to hand them to one cipher and then discard them, such as
     * {@link SFrameKeyProvider} expanding a chain key. The public {@link #SFrameCipher(SFrameCipherSuite,
     * byte[], byte[], byte[]) constructor} defensively clones its arguments to guard against a caller
     * that retains and later mutates them; when the caller instead transfers sole ownership of
     * just derived arrays, those clones are redundant, so this factory stores the arrays directly. The
     * caller must neither retain nor mutate {@code aesKey}, {@code authKey}, or {@code counterMask}
     * after the call.
     *
     * @param suite       the cipher suite carrying the cipher variant and tag length
     * @param aesKey      the freshly derived {@code AES-128} encryption key, ownership transferred
     * @param authKey     the freshly derived {@code HMAC-SHA256} authentication key, ownership transferred
     * @param counterMask the freshly derived 16 byte counter mask, ownership transferred
     * @return a cipher backed directly by the given arrays
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code aesKey}, {@code authKey}, or {@code counterMask} has
     *                                  the wrong length
     */
    static SFrameCipher ofOwned(SFrameCipherSuite suite, byte[] aesKey, byte[] authKey, byte[] counterMask) {
        return new SFrameCipher(suite, aesKey, authKey, counterMask, true);
    }

    /**
     * Constructs a cipher with an all zero counter mask.
     *
     * <p>This is the no salt form: every counter block is the AES encryption of the big endian counter
     * with no mask XOR, which is what the call media path does until the per key id salt that feeds the
     * real mask is recovered.
     *
     * @param suite   the cipher suite carrying the cipher variant and tag length
     * @param aesKey  the {@code AES-128} encryption key
     * @param authKey the {@code HMAC-SHA256} authentication key
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code aesKey} or {@code authKey} has the wrong length
     */
    public SFrameCipher(SFrameCipherSuite suite, byte[] aesKey, byte[] authKey) {
        this(suite, aesKey, authKey, zeroCounterMask());
    }

    /**
     * Returns a fresh all zero counter mask.
     *
     * <p>This is the mask for a cipher with no per key id salt installed; XORing it leaves the AES
     * encrypted counter block unchanged.
     *
     * @return a new {@value SFrameCipherSuite#IV_LENGTH} byte all zero array
     */
    public static byte[] zeroCounterMask() {
        // TODO: install the real 12 byte counter mask salt once the native ratchet is recovered. RE
        //  (re/calls) established the salt is the 12 bytes contiguous after the 16 byte AES key in the native
        //  WASframeAESCipher's 28 byte cipher key, so it is a derived value, not zero; see
        //  SFrameKeyProvider.deriveCipher for the recovered layout and the remaining unknown (the
        //  facebook::sframe ratchet HKDF). Until that lands the provider installs this all zero mask, which
        //  keeps encrypt and decrypt mutually consistent but is not byte compatible with native WhatsApp.
        return new byte[SFrameCipherSuite.IV_LENGTH];
    }

    /**
     * Builds the 16 byte counter mask from a per key id salt.
     *
     * <p>The mask is the salt bytes copied into a zero initialized 16 byte buffer. A salt shorter than
     * {@value SFrameCipherSuite#IV_LENGTH} bytes leaves the trailing bytes zero; a salt of
     * {@value SFrameCipherSuite#IV_LENGTH} or more bytes is truncated to the mask width. The native
     * derivation reads {@value #SALT_LENGTH} salt bytes, so a {@value #SALT_LENGTH} byte salt yields a
     * mask whose last four bytes are zero.
     *
     * @param salt the per key id salt, at most {@value SFrameCipherSuite#IV_LENGTH} bytes consumed
     * @return the {@value SFrameCipherSuite#IV_LENGTH} byte counter mask
     * @throws NullPointerException if {@code salt} is {@code null}
     */
    public static byte[] counterMaskFromSalt(byte[] salt) {
        Objects.requireNonNull(salt, "salt cannot be null");
        var mask = new byte[SFrameCipherSuite.IV_LENGTH];
        System.arraycopy(salt, 0, mask, 0, Math.min(salt.length, SFrameCipherSuite.IV_LENGTH));
        return mask;
    }

    /**
     * Returns the cipher suite this cipher applies.
     *
     * @return the cipher suite
     */
    public SFrameCipherSuite suite() {
        return suite;
    }

    /**
     * Returns the truncated authentication tag length, in bytes, this cipher appends.
     *
     * @return the tag length from the {@linkplain #suite() suite}
     */
    public int tagLength() {
        return suite.tagLength();
    }

    /**
     * Seals one frame by encrypting {@code plaintext} with {@code AES-CTR} under the counter block for
     * {@code counter} and appending the truncated {@code HMAC-SHA256} over {@code trailer || ciphertext}.
     *
     * @param trailer   the SFrame trailer bytes covered as associated data
     * @param plaintext the frame plaintext to encrypt
     * @param counter   the per frame counter selecting the counter block
     * @return the ciphertext followed by the {@link #tagLength()} byte tag
     * @throws NullPointerException       if {@code trailer} or {@code plaintext} is {@code null}
     * @throws WhatsAppCallException.Srtp if the AES or HMAC computation fails
     */
    public byte[] seal(byte[] trailer, byte[] plaintext, long counter) {
        Objects.requireNonNull(trailer, "trailer cannot be null");
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        var out = new byte[plaintext.length + suite.tagLength()];
        sealInto(trailer, plaintext, counter, out, 0);
        return out;
    }

    /**
     * Seals one frame directly into a caller supplied buffer, writing the ciphertext followed by the
     * truncated tag at {@code outOffset} without allocating an intermediate frame array.
     *
     * <p>The buffer region {@code [outOffset, outOffset + plaintext.length + tagLength)} must be free.
     * The ciphertext is written first, then the HMAC is computed over {@code trailer} followed by that
     * just written ciphertext, and the leading {@link #tagLength()} bytes of the HMAC are copied in
     * immediately after it. This lets {@link SFrameSecureFrame} lay the cipher output straight into the
     * final frame buffer and append the trailer in place, fusing the encrypt and tag append that
     * {@link #seal(byte[], byte[], long)} otherwise stitches together with a copy.
     *
     * @param trailer   the SFrame trailer bytes covered as associated data
     * @param plaintext the frame plaintext to encrypt
     * @param counter   the per frame counter selecting the counter block
     * @param out       the destination buffer receiving the ciphertext and tag
     * @param outOffset the offset in {@code out} at which the ciphertext begins
     * @return the number of bytes written, {@code plaintext.length + tagLength()}
     * @throws NullPointerException       if {@code trailer}, {@code plaintext}, or {@code out} is
     *                                    {@code null}
     * @throws WhatsAppCallException.Srtp if the AES or HMAC computation fails
     */
    int sealInto(byte[] trailer, byte[] plaintext, long counter, byte[] out, int outOffset) {
        Objects.requireNonNull(trailer, "trailer cannot be null");
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        Objects.requireNonNull(out, "out cannot be null");
        if (Log.TRACE) LOGGER.log(Level.TRACE, "sframe seal counter={0} ptLen={1} tagLen={2}", counter, plaintext.length, suite.tagLength());
        var counterBlock = counterBlock(counter);
        aesCtrInto(plaintext, counterBlock, out, outOffset);
        var tagLength = suite.tagLength();
        if (tagLength > 0) {
            var tag = authTag(trailer, 0, trailer.length, out, outOffset, plaintext.length);
            System.arraycopy(tag, 0, out, outOffset + plaintext.length, tagLength);
        }
        return plaintext.length + tagLength;
    }

    /**
     * Opens one frame by verifying the trailing tag against the HMAC over {@code trailer || ciphertext},
     * then decrypting the body with {@code AES-CTR}.
     *
     * <p>Returns {@code null} when the body is shorter than the tag or the tag does not verify; the
     * caller drops the frame in those cases. The HMAC is verified before the decrypt.
     *
     * @param trailer the SFrame trailer bytes covered as associated data
     * @param body    the ciphertext followed by the {@link #tagLength()} byte tag
     * @param counter the per frame counter selecting the counter block
     * @return the recovered plaintext, or {@code null} if the body is too short or authentication
     *         fails
     * @throws NullPointerException       if {@code trailer} or {@code body} is {@code null}
     * @throws WhatsAppCallException.Srtp if the AES or HMAC computation fails
     */
    public byte[] open(byte[] trailer, byte[] body, long counter) {
        Objects.requireNonNull(trailer, "trailer cannot be null");
        Objects.requireNonNull(body, "body cannot be null");
        var tagLength = suite.tagLength();
        if (body.length < tagLength) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe open: body shorter than tag, counter={0} bodyLen={1} tagLen={2}", counter, body.length, tagLength);
            return null;
        }
        var ciphertextLength = body.length - tagLength;
        if (tagLength > 0) {
            var receivedTag = Arrays.copyOfRange(body, ciphertextLength, body.length);
            var expectedTag = authTag(trailer, 0, trailer.length, body, 0, ciphertextLength);
            if (!MessageDigest.isEqual(expectedTag, receivedTag)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe open: authentication failed, dropping frame, counter={0}", counter);
                return null;
            }
        }
        return aesCtr(body, 0, ciphertextLength, counterBlock(counter));
    }

    /**
     * Opens one frame still laid out as {@code [ciphertext][tag]} in the leading {@code trailerStart}
     * bytes of {@code frame}, with the trailer occupying the remainder, without slicing the body or the
     * trailer out first.
     *
     * <p>This is the fused counterpart of {@link #open(byte[], byte[], long)} for the
     * {@link SFrameSecureFrame} decode path: the body is {@code frame[0, trailerStart)} and the
     * trailer associated data is {@code frame[trailerStart, frame.length)}, two contiguous
     * non overlapping regions of the one received array, so neither region is copied out. The trailing
     * tag is verified against the HMAC over {@code trailer || ciphertext} before the decrypt,
     * exactly as {@link #open(byte[], byte[], long)} does over the equivalent standalone arrays.
     *
     * @param frame        the received frame, body then trailer, with no separate copies
     * @param trailerStart the offset in {@code frame} where the trailer begins and the body ends
     * @param counter      the per frame counter selecting the counter block
     * @return the recovered plaintext, or {@code null} if the body is too short or authentication fails
     * @throws WhatsAppCallException.Srtp if the AES or HMAC computation fails
     */
    byte[] openFrame(byte[] frame, int trailerStart, long counter) {
        var tagLength = suite.tagLength();
        if (trailerStart < tagLength) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe openFrame: body shorter than tag, counter={0} trailerStart={1} tagLen={2}", counter, trailerStart, tagLength);
            return null;
        }
        var ciphertextLength = trailerStart - tagLength;
        if (tagLength > 0) {
            var receivedTag = Arrays.copyOfRange(frame, ciphertextLength, trailerStart);
            var expectedTag =
                    authTag(frame, trailerStart, frame.length - trailerStart, frame, 0, ciphertextLength);
            if (!MessageDigest.isEqual(expectedTag, receivedTag)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "sframe openFrame: authentication failed, dropping frame, counter={0}", counter);
                return null;
            }
        }
        return aesCtr(frame, 0, ciphertextLength, counterBlock(counter));
    }

    /**
     * Builds the 16 byte {@code AES-CTR} initial block for a frame counter.
     *
     * <p>The counter is serialized big endian into the middle eight bytes of an otherwise zero
     * 16 byte block, encrypted under the AES key, then XORed byte for byte with the stored
     * {@link #counterMask}. The block layout is four zero bytes, the eight big endian counter bytes,
     * then four zero bytes:
     *
     * {@snippet :
     *   block[0 .. 4]   = 0x00000000           // four leading zero bytes
     *   block[4 .. 12]  = bigEndian(counter)   // eight counter bytes
     *   block[12 .. 16] = 0x00000000           // four trailing zero bytes
     * }
     *
     * @param counter the per frame counter
     * @return the 16 byte CTR initial block
     * @throws WhatsAppCallException.Srtp if the AES encryption of the counter block fails
     */
    private byte[] counterBlock(long counter) {
        var block = counterScratch;
        for (var i = 0; i < Long.BYTES; i++) {
            block[Long.BYTES + 3 - i] = (byte) (counter >>> (8 * i));
        }
        var encrypted = aesEncryptBlock(block);
        for (var i = 0; i < SFrameCipherSuite.IV_LENGTH; i++) {
            encrypted[i] ^= counterMask[i];
        }
        return encrypted;
    }

    /**
     * Runs {@code AES-128} counter mode (its own inverse) over {@code length} bytes of {@code data}
     * starting at {@code offset}, from the given CTR initial block.
     *
     * @param data        the buffer holding the bytes to transform
     * @param offset      the offset in {@code data} at which the region begins
     * @param length      the number of bytes to transform
     * @param initalBlock the 16 byte CTR initial counter block
     * @return the transformed bytes, a fresh array of {@code length} bytes
     * @throws WhatsAppCallException.Srtp if the AES provider rejects the inputs
     */
    private byte[] aesCtr(byte[] data, int offset, int length, byte[] initalBlock) {
        try {
            ctrCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, new IvParameterSpec(initalBlock));
            return ctrCipher.doFinal(data, offset, length);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sframe aes-ctr failed", e);
            throw new WhatsAppCallException.Srtp("SFrame AES-CTR failed", e);
        }
    }

    /**
     * Runs {@code AES-128} counter mode over all of {@code data} from the given CTR initial block,
     * writing the transformed bytes into {@code out} at {@code outOffset} rather than returning a fresh
     * array.
     *
     * @param data        the bytes to transform
     * @param initalBlock the 16 byte CTR initial counter block
     * @param out         the destination buffer receiving the transformed bytes
     * @param outOffset   the offset in {@code out} at which the output begins
     * @throws WhatsAppCallException.Srtp if the AES provider rejects the inputs or {@code out} is too
     *                                    small
     */
    private void aesCtrInto(byte[] data, byte[] initalBlock, byte[] out, int outOffset) {
        try {
            ctrCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, new IvParameterSpec(initalBlock));
            ctrCipher.doFinal(data, 0, data.length, out, outOffset);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sframe aes-ctr (into) failed", e);
            throw new WhatsAppCallException.Srtp("SFrame AES-CTR failed", e);
        }
    }

    /**
     * AES encrypts a single 16 byte block in ECB mode, the primitive that turns each per frame counter
     * block into the pre mask CTR keystream block.
     *
     * @param block the 16 byte block to encrypt
     * @return the 16 byte AES encrypted block
     * @throws WhatsAppCallException.Srtp if the AES provider rejects the inputs
     */
    private byte[] aesEncryptBlock(byte[] block) {
        try {
            blockCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec);
            return blockCipher.doFinal(block);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sframe aes block encryption failed", e);
            throw new WhatsAppCallException.Srtp("SFrame AES block encryption failed", e);
        }
    }

    /**
     * Computes the leading {@link #tagLength()} bytes of the {@code HMAC-SHA256} over the
     * {@code aadLength} trailer bytes at {@code aadOffset} followed by the {@code ciphertextLength}
     * ciphertext bytes at {@code ciphertextOffset}.
     *
     * <p>The trailer is the only associated data; there is no nonce or length suffix in the HMAC
     * input. The two regions are hashed in order (trailer then ciphertext), so passing them as offset
     * ranges of the same or different buffers yields the same tag as hashing standalone {@code trailer}
     * and {@code ciphertext} arrays.
     *
     * @param aad              the buffer holding the SFrame trailer associated data bytes
     * @param aadOffset        the offset in {@code aad} at which the trailer begins
     * @param aadLength        the number of trailer bytes
     * @param ciphertext       the buffer holding the ciphertext bytes
     * @param ciphertextOffset the offset in {@code ciphertext} at which the ciphertext begins
     * @param ciphertextLength the number of ciphertext bytes
     * @return the truncated authentication tag
     * @throws WhatsAppCallException.Srtp if the HMAC provider rejects the inputs
     */
    private byte[] authTag(byte[] aad, int aadOffset, int aadLength,
                           byte[] ciphertext, int ciphertextOffset, int ciphertextLength) {
        try {
            hmac.init(macKeySpec);
            hmac.update(aad, aadOffset, aadLength);
            hmac.update(ciphertext, ciphertextOffset, ciphertextLength);
            var full = hmac.doFinal();
            return Arrays.copyOf(full, suite.tagLength());
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sframe hmac failed", e);
            throw new WhatsAppCallException.Srtp("SFrame HMAC failed", e);
        }
    }
}
