package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.Mac;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Common base for the WhatsApp media encryption and decryption stream
 * pipelines.
 *
 * <p>This type holds the cryptographic primitives shared by
 * {@link MediaUploadInputStream} (encrypt-then-HMAC) and
 * {@link MediaDownloadInputStream} (verify-then-decrypt): HKDF-SHA256 key
 * derivation, AES-CBC cipher creation with PKCS5 padding, HMAC-SHA256
 * initialisation, and SHA-256 hashing. Subclasses drive a streaming state
 * machine; this base contributes only the per-operation factory methods and
 * the raw input wiring. The concrete subclasses are reached through
 * {@link MediaConnectionService#upload(com.github.auties00.cobalt.wire.linked.media.MediaProvider, MediaPayload)}
 * and {@link MediaConnectionService#download(com.github.auties00.cobalt.wire.linked.media.MediaProvider)}.
 */
@WhatsAppWebModule(moduleName = "WAMediaCrypto")
abstract class MediaInputStream extends InputStream {
    /** The logger for {@link MediaInputStream}. */
    private static final System.Logger LOGGER = Log.get(MediaInputStream.class);

    /**
     * The streaming buffer size in bytes used for chunked reads and cipher
     * operations.
     *
     * <p>Chosen as a multiple of the AES block size so that streaming
     * {@link Cipher#update(byte[], int, int, byte[], int)} calls produce
     * output that fits in the staging buffer without partial-block carry.
     *
     * @implNote
     * This implementation uses 8192 bytes, which is also WA Web's 64 KiB
     * chunk size divided into eight {@code update} passes; the size keeps
     * the encrypt/decrypt loop interactive on virtual threads.
     */
    static final int BUFFER_LENGTH = 8192;

    /**
     * The number of trailing HMAC bytes appended to the ciphertext on the
     * wire.
     *
     * <p>WhatsApp computes a full 32-byte HMAC-SHA256 over
     * {@code IV || ciphertext} but publishes only the first 10 bytes. The
     * truncated tag preserves integrity while reducing the on-wire overhead
     * on media that is often transferred over constrained mobile links.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "HMAC_LENGTH",
            adaptation = WhatsAppAdaptation.DIRECT)
    static final int MAC_LENGTH = 10;

    /**
     * The total size in bytes of the HKDF-expanded media key material.
     *
     * <p>The expanded buffer is partitioned as:
     * <ul>
     *   <li>bytes {@code 0..15}: AES-CBC initialisation vector</li>
     *   <li>bytes {@code 16..47}: AES-CBC cipher key</li>
     *   <li>bytes {@code 48..79}: HMAC-SHA256 key</li>
     *   <li>bytes {@code 80..111}: reference key reserved for media previews</li>
     * </ul>
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "computeMediaKeys",
            adaptation = WhatsAppAdaptation.DIRECT)
    static final int EXPANDED_SIZE = 112;

    /**
     * The symmetric key length in bytes used for both the AES cipher key
     * and the HMAC key.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "computeMediaKeys",
            adaptation = WhatsAppAdaptation.DIRECT)
    static final int KEY_LENGTH = 32;

    /**
     * The initialisation vector length in bytes for AES-CBC.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "IV_LENGTH",
            adaptation = WhatsAppAdaptation.DIRECT)
    static final int IV_LENGTH = 16;

    /**
     * The AES-CBC block size in bytes.
     *
     * <p>Used to size the ciphertext staging buffer so that
     * {@link Cipher#doFinal(byte[], int)} can emit the trailing PKCS5
     * padding block without reallocation.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "CBC_BLOCK_SIZE",
            adaptation = WhatsAppAdaptation.DIRECT)
    static final int CBC_BLOCK_SIZE = 16;

    /**
     * The underlying raw input stream providing the source bytes.
     *
     * <p>Carries plaintext to be encrypted in the upload pipeline and the
     * ciphertext-plus-HMAC payload delivered by the CDN in the download
     * pipeline.
     */
    final InputStream rawInputStream;

    /**
     * Constructs a new media input stream wrapping the given raw stream.
     *
     * <p>Invoked from the subclass constructors of
     * {@link MediaUploadInputStream} and {@link MediaDownloadInputStream}.
     *
     * @param rawInputStream the underlying input stream
     * @throws NullPointerException if {@code rawInputStream} is {@code null}
     */
    MediaInputStream(InputStream rawInputStream) {
        this.rawInputStream = Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
    }

    /**
     * Derives the expanded key material for a media payload from the raw
     * 32-byte media key and the media-type specific info string.
     *
     * <p>Callers slice the returned {@value #EXPANDED_SIZE}-byte array into
     * the IV, cipher key, HMAC key, and reference key by indexing into the
     * partition layout documented on {@link #EXPANDED_SIZE}. Typical
     * {@code mediaKeyName} values are {@code "WhatsApp Image Keys"},
     * {@code "WhatsApp Video Keys"}, {@code "WhatsApp Audio Keys"}, and
     * {@code "WhatsApp Document Keys"}.
     *
     * @implNote
     * This implementation performs HKDF-SHA256 extract-then-expand with no
     * explicit salt (defaulting to 32 zero bytes per RFC 5869) and the
     * UTF-8 encoding of the key name as the info parameter.
     *
     * @param mediaKey     the 32-byte raw media key
     * @param mediaKeyName the HKDF info string identifying the media type
     * @return the expanded key material
     * @throws WhatsAppMediaException if the HKDF derivation fails
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "computeMediaKeys",
            adaptation = WhatsAppAdaptation.DIRECT)
    byte[] deriveMediaKeyData(byte[] mediaKey, String mediaKeyName) throws WhatsAppMediaException {
        try {
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(mediaKey)
                    .thenExpand(mediaKeyName.getBytes(StandardCharsets.UTF_8), EXPANDED_SIZE);
            return hkdf.deriveData(params);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "media key derivation failed for " + mediaKeyName, e);
            throw new WhatsAppMediaException("Cannot derive media key data", e);
        }
    }

    /**
     * Creates a fresh SHA-256 {@link MessageDigest} for plaintext or
     * ciphertext hashing.
     *
     * <p>Used by both pipelines to compute the {@code fileSha256} and
     * {@code fileEncSha256} fields recorded on the outgoing media message
     * protobuf, and on download to verify the recipient-side plaintext and
     * ciphertext digests against the values advertised by the sender.
     *
     * @return a fresh SHA-256 digest
     * @throws WhatsAppMediaException if SHA-256 is unavailable on the JVM
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto",
            exports = {"encryptAndHmac", "hmacAndDecrypt"},
            adaptation = WhatsAppAdaptation.DIRECT)
    MessageDigest newHash() throws WhatsAppMediaException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "sha-256 digest unavailable", exception);
            throw new WhatsAppMediaException("Cannot create new hash", exception);
        }
    }

    /**
     * Creates and initialises a new AES-CBC cipher with PKCS5 padding.
     *
     * <p>Drives both the encrypt branch of the upload pipeline and the
     * decrypt branch of the download pipeline; the {@code mode} flag
     * decides which.
     *
     * @param mode the cipher mode, either {@link Cipher#ENCRYPT_MODE} or
     *             {@link Cipher#DECRYPT_MODE}
     * @param key  the AES secret key
     * @param iv   the initialisation vector
     * @return the initialised cipher
     * @throws WhatsAppMediaException if cipher creation or initialisation
     *         fails
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto",
            exports = {"encryptAndHmac", "hmacAndDecrypt"},
            adaptation = WhatsAppAdaptation.DIRECT)
    Cipher newCipher(int mode, SecretKeySpec key, IvParameterSpec iv) throws WhatsAppMediaException {
        try {
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(mode, key, iv);
            return cipher;
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "aes-cbc cipher init failed for mode " + mode, exception);
            throw new WhatsAppMediaException("Cannot create new cipher", exception);
        }
    }

    /**
     * Creates and initialises a new HMAC-SHA256 instance bound to the
     * supplied HMAC key.
     *
     * <p>The returned MAC is primed by the subclasses with the AES IV
     * before any ciphertext is fed in, so the final tag covers
     * {@code IV || ciphertext} as required by the WhatsApp wire format.
     *
     * @param key the HMAC-SHA256 secret key
     * @return the initialised MAC
     * @throws WhatsAppMediaException if MAC creation or initialisation fails
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto",
            exports = {"encryptAndHmac", "hmacAndDecrypt"},
            adaptation = WhatsAppAdaptation.DIRECT)
    Mac newMac(SecretKeySpec key) throws WhatsAppMediaException {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac;
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "hmac-sha256 init failed", exception);
            throw new WhatsAppMediaException("Cannot create new mac", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation closes only the underlying
     * {@link #rawInputStream}; subclasses release additional resources
     * (HTTP client, zlib inflater) by extending this method.
     */
    @Override
    public void close() throws IOException {
        rawInputStream.close();
    }
}
