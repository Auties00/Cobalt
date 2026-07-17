package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Streams a piece of media destined for WhatsApp's CDN, encrypting it on
 * the fly and capturing the integrity hashes that accompany the outgoing
 * message.
 *
 * <p>WhatsApp transfers two kinds of media: end-to-end encrypted
 * attachments such as images, videos, audio, documents, and stickers; and
 * unencrypted assets such as newsletter media, profile pictures, and
 * business cover photos. {@link Ciphertext} produces the encrypted form
 * for the first group ({@code ciphertext || HMAC[0:10]} with AES-CBC and
 * truncated HMAC-SHA256 over {@code IV || ciphertext}) and
 * {@link Plaintext} passes the second group through while still computing
 * the SHA-256 hash that the server expects on the upload metadata.
 *
 * <p>Constructed indirectly by
 * {@link MediaConnectionService#upload(MediaProvider, MediaPayload)}
 * through {@link #of(MediaProvider, InputStream)}. Once the stream has been
 * fully consumed, callers query {@link #fileSha256()},
 * {@link #fileEncSha256()}, {@link #fileKey()}, and {@link #fileLength()}
 * to populate the outgoing message protobuf.
 *
 * @implNote
 * This implementation is a streaming adapter over WA Web's batch
 * {@code encryptAndHmac}: WA Web encrypts the full plaintext in memory and
 * emits one {@code ArrayBuffer}, whereas Cobalt produces ciphertext chunk
 * by chunk so large attachments can be piped to a temporary file without
 * holding the whole payload in heap.
 */
@WhatsAppWebModule(moduleName = "WAMediaCrypto")
@WhatsAppWebModule(moduleName = "WAWebCryptoEncryptMedia")
abstract sealed class MediaUploadInputStream extends MediaInputStream {
    /** The logger for {@link MediaUploadInputStream}. */
    private static final System.Logger LOGGER = Log.get(MediaUploadInputStream.class);

    /**
     * Constructs a new upload stream wrapping the given raw plaintext
     * stream.
     *
     * <p>Subclass-only constructor invoked by {@link Ciphertext} and
     * {@link Plaintext} via {@link #of(MediaProvider, InputStream)}.
     *
     * @param rawInputStream the underlying plaintext input stream
     */
    MediaUploadInputStream(InputStream rawInputStream) {
        super(rawInputStream);
    }

    /**
     * Returns the total number of plaintext bytes processed from the
     * underlying stream.
     *
     * <p>Recorded into the outgoing message protobuf's {@code fileLength}
     * field so that recipients can pre-allocate buffers and report accurate
     * progress. Stable only after the upload stream has been fully
     * consumed.
     *
     * @implSpec
     * Implementations must return the total number of bytes read from the
     * underlying plaintext stream so far; partial reads observed during
     * upload are permitted to return a running count.
     *
     * @return the plaintext file length in bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    public abstract long fileLength();

    /**
     * Returns the SHA-256 digest of the plaintext content.
     *
     * <p>Available only after the stream has been fully consumed (a
     * {@code read} returned {@code -1}). The value populates the outgoing
     * message protobuf's {@code fileSha256} field so that recipients can
     * verify the decrypted bytes after download.
     *
     * @implSpec
     * Implementations must throw {@link IllegalStateException} when the
     * stream has not yet been fully consumed; the digest must not be
     * truncated to fewer than 32 bytes.
     *
     * @return the plaintext SHA-256 hash
     * @throws IllegalStateException if the stream has not been fully
     *         consumed
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    public abstract byte[] fileSha256();

    /**
     * Returns the SHA-256 digest of the encrypted payload
     * ({@code ciphertext || HMAC[0:10]}).
     *
     * <p>Available only after the stream has been fully consumed. Populates
     * the outgoing message protobuf's {@code fileEncSha256} field. Empty
     * for plaintext uploads that skip encryption.
     *
     * @implSpec
     * Implementations that perform encryption must throw
     * {@link IllegalStateException} when the stream has not yet been
     * fully consumed; plaintext implementations always return
     * {@link Optional#empty()}.
     *
     * @return an {@link Optional} holding the encrypted SHA-256 hash, or
     *         empty for unencrypted media
     * @throws IllegalStateException if the stream has not been fully
     *         consumed (encrypted variant only)
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    public abstract Optional<byte[]> fileEncSha256();

    /**
     * Returns the 32-byte random media key used as the HKDF seed.
     *
     * <p>The media key is stored in the outgoing message protobuf so that
     * recipients can reproduce the same IV, cipher key, and HMAC key when
     * decrypting. Empty for plaintext uploads.
     *
     * @implSpec
     * Implementations that perform encryption must return a 32-byte
     * randomly-generated key; plaintext implementations return
     * {@link Optional#empty()}.
     *
     * @return an {@link Optional} holding the media key, or empty for
     *         unencrypted media
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    public abstract Optional<byte[]> fileKey();

    /**
     * Creates the appropriate upload stream variant for the supplied
     * provider.
     *
     * <p>Returns a {@link Ciphertext} stream when the provider's media path
     * advertises a key name (end-to-end encrypted media) or a
     * {@link Plaintext} stream otherwise (newsletter media, profile
     * pictures, business cover photos).
     *
     * @param provider    the media provider describing the media type
     * @param inputStream the raw plaintext input stream
     * @return the appropriate upload stream
     * @throws WhatsAppMediaException if cipher initialisation fails
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    static MediaUploadInputStream of(MediaProvider provider, InputStream inputStream) throws WhatsAppMediaException {
        return of(provider, inputStream, null);
    }

    /**
     * Creates the appropriate upload stream variant for the supplied
     * provider with a caller-supplied media key.
     *
     * <p>Used by the two-pass upload in
     * {@link MediaConnectionService#upload(MediaProvider, MediaPayload)}:
     * pass 1 generates the random 32-byte media key, pass 2 (and every
     * retry) reuses it so the ciphertext bytes remain identical between
     * attempts and match the {@code fileEncSha256} captured during pass 1.
     * When the provider does not request encryption (newsletter media,
     * profile pictures, business cover photos) {@code mediaKey} is ignored.
     *
     * @param provider    the media provider describing the media type
     * @param inputStream the raw plaintext input stream
     * @param mediaKey    the 32-byte media key to reuse, or {@code null}
     *                    to generate a fresh one (only consulted when the
     *                    provider's media path declares a key name)
     * @return the appropriate upload stream
     * @throws WhatsAppMediaException if cipher initialisation fails
     */
    static MediaUploadInputStream of(MediaProvider provider, InputStream inputStream,
                                     byte[] mediaKey) throws WhatsAppMediaException {
        var keyName = provider.mediaPath()
                .keyName();
        if (keyName.isPresent()) {
            return mediaKey == null
                    ? new Ciphertext(inputStream, keyName.get())
                    : new Ciphertext(inputStream, keyName.get(), mediaKey);
        }
        return new Plaintext(inputStream);
    }

    /**
     * An encrypted upload stream that performs AES-CBC encryption with a
     * trailing truncated HMAC-SHA256 authentication tag.
     *
     * <p>The pipeline generates a random 32-byte media key, derives the IV,
     * cipher key, and HMAC key via HKDF-SHA256, encrypts the plaintext with
     * AES-CBC, computes HMAC-SHA256 over {@code IV || ciphertext} truncated
     * to {@link #MAC_LENGTH} bytes, and emits {@code ciphertext || HMAC[0:10]}
     * on the wire while accumulating SHA-256 over both the plaintext and the
     * encrypted output.
     */
    @WhatsAppWebModule(moduleName = "WAMediaCrypto")
    @WhatsAppWebModule(moduleName = "WAWebCryptoEncryptMedia")
    private static final class Ciphertext extends MediaUploadInputStream {
        /**
         * Accumulates SHA-256 over the plaintext as bytes are read from the
         * underlying stream.
         *
         * <p>The final value is exposed by {@link #fileSha256()}.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final MessageDigest plaintextDigest;

        /**
         * Accumulates SHA-256 over the encrypted output
         * ({@code ciphertext || HMAC[0:10]}).
         *
         * <p>The final value is exposed by {@link #fileEncSha256()}.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final MessageDigest ciphertextDigest;

        /**
         * The HMAC-SHA256 instance computing the authentication tag over
         * {@code IV || ciphertext}.
         *
         * <p>Only the first {@link #MAC_LENGTH} bytes of the final tag are
         * written to the output stream.
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final Mac ciphertextMac;

        /**
         * The AES-CBC cipher in encrypt mode.
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final Cipher cipher;

        /**
         * Scratch buffer for staging plaintext chunks before passing them
         * to the cipher.
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private final byte[] plaintextBuffer;

        /**
         * Scratch buffer for the cipher output.
         *
         * <p>Sized to accommodate one extra AES block so that
         * {@link Cipher#doFinal(byte[], int)} can emit its trailing PKCS5
         * padding block without reallocation.
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private final byte[] ciphertextBuffer;

        /**
         * The output buffer exposed to callers via {@link #read()} and
         * {@link #read(byte[], int, int)}.
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private final byte[] outputBuffer;

        /**
         * The randomly generated 32-byte media key seeding the HKDF
         * derivation.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final byte[] mediaKey;

        /**
         * The finalised SHA-256 digest of the plaintext.
         *
         * <p>Populated by {@link #ensureDataAvailable()} when the
         * underlying stream is exhausted, and surfaced by
         * {@link #fileSha256()}.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private byte[] plaintextHash;

        /**
         * The finalised SHA-256 digest of {@code ciphertext || HMAC[0:10]}.
         *
         * <p>Populated by {@link #ensureDataAvailable()} when the
         * underlying stream is exhausted, and surfaced by
         * {@link #fileEncSha256()}.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private byte[] ciphertextHash;

        /**
         * The running count of plaintext bytes read from the underlying
         * stream.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private long plaintextLength;

        /**
         * Tracks whether the stream has been fully processed and the
         * trailing HMAC plus hashes have been emitted.
         */
        private boolean finalized;

        /**
         * The current read cursor within {@link #outputBuffer}.
         */
        private int outputPosition;

        /**
         * The number of valid bytes currently held by {@link #outputBuffer}.
         */
        private int outputLimit;

        /**
         * Constructs an encrypted upload stream for the given media type.
         *
         * <p>Invoked from {@link #of(MediaProvider, InputStream)} for media
         * paths whose {@code keyName} is present.
         *
         * @implNote
         * This implementation generates a random 32-byte media key,
         * HKDF-derives the IV, cipher key, and HMAC key, initialises the
         * AES-CBC cipher in encrypt mode, and primes the HMAC with the IV
         * bytes so that the authentication tag covers
         * {@code IV || ciphertext}.
         *
         * @param rawInputStream the plaintext input stream to encrypt
         * @param keyName        the HKDF info string for the media type
         * @throws WhatsAppMediaException if key derivation or cipher
         *         initialisation fails
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public Ciphertext(InputStream rawInputStream, String keyName) throws WhatsAppMediaException {
            this(rawInputStream, keyName, DataUtils.randomByteArray(32));
        }

        /**
         * Constructs an encrypted upload stream with a caller-provided
         * media key.
         *
         * <p>Used by the two-pass upload in
         * {@link MediaConnectionService#upload(MediaProvider, MediaPayload)}
         * so the second pass produces the same ciphertext as the first
         * (matching the {@code fileEncSha256} embedded in the upload URL).
         *
         * @param rawInputStream the plaintext input stream to encrypt
         * @param keyName        the HKDF info string for the media type
         * @param mediaKey       the 32-byte media key to reuse
         * @throws WhatsAppMediaException if key derivation or cipher
         *         initialisation fails
         */
        public Ciphertext(InputStream rawInputStream, String keyName, byte[] mediaKey)
                throws WhatsAppMediaException {
            super(rawInputStream);

            this.plaintextDigest = newHash();
            this.ciphertextDigest = newHash();

            this.mediaKey = mediaKey;

            var expanded = deriveMediaKeyData(mediaKey, keyName);
            var iv = new IvParameterSpec(expanded, 0, IV_LENGTH);
            var cipherKey = new SecretKeySpec(expanded, IV_LENGTH, KEY_LENGTH, "AES");
            var macKey = new SecretKeySpec(expanded, IV_LENGTH + KEY_LENGTH, KEY_LENGTH, "HmacSHA256");

            this.cipher = newCipher(Cipher.ENCRYPT_MODE, cipherKey, iv);
            this.ciphertextMac = newMac(macKey);

            ciphertextMac.update(expanded, 0, IV_LENGTH);

            this.plaintextBuffer = new byte[BUFFER_LENGTH];

            // BUFFER_LENGTH is block-aligned (8192 == 512 * 16) so streaming
            // cipher.update never carries a partial block; the extra block
            // here holds doFinal's PKCS5 padding output
            this.ciphertextBuffer = new byte[BUFFER_LENGTH + CBC_BLOCK_SIZE];
            this.outputBuffer = new byte[BUFFER_LENGTH];
            this.plaintextLength = 0;

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "encrypted media upload stream initialized, keyName={0}", keyName);
        }

        /**
         * Reads a single byte of the encrypted payload.
         *
         * <p>Drives {@link #ensureDataAvailable()} to stage the next chunk
         * on demand.
         *
         * @return the next byte of encrypted data, or {@code -1} if the
         *         stream is exhausted
         * @throws IOException if an I/O or encryption error occurs
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @Override
        public int read() throws IOException {
            ensureDataAvailable();
            if (outputPosition >= outputLimit) {
                return -1;
            }

            return outputBuffer[outputPosition++] & 0xFF;
        }

        /**
         * Reads up to {@code len} bytes of the encrypted payload into the
         * supplied array.
         *
         * <p>Drives {@link #ensureDataAvailable()} to stage the next chunk
         * on demand; returns as soon as the staging buffer has data, up to
         * {@code len} bytes.
         *
         * @param b   the destination buffer
         * @param off the start offset in the destination buffer
         * @param len the maximum number of bytes to read
         * @return the number of bytes actually read, or {@code -1} if the
         *         stream is exhausted
         * @throws IOException if an I/O or encryption error occurs
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureDataAvailable();
            if (outputPosition >= outputLimit) {
                return -1;
            }

            var available = outputLimit - outputPosition;
            var toRead = Math.min(len, available);
            System.arraycopy(outputBuffer, outputPosition, b, off, toRead);
            outputPosition += toRead;
            return toRead;
        }

        /**
         * Refills {@link #outputBuffer} with encrypted data from the
         * underlying stream.
         *
         * <p>Each pass reads a plaintext chunk, encrypts it with AES-CBC,
         * extends the HMAC and digest accumulators, and copies the
         * ciphertext into the output buffer. On end-of-stream the cipher is
         * finalised, the truncated HMAC is appended, and the SHA-256 hashes
         * are captured for the accessor methods.
         *
         * @implNote
         * This implementation appends the truncated HMAC into the same
         * staging buffer that exposes encrypted bytes to the caller, so
         * the wire layout {@code ciphertext || HMAC[0:10]} is produced
         * without an additional copy.
         *
         * @throws IOException if an I/O or encryption error occurs
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private void ensureDataAvailable() throws IOException {
            try {
                while (outputPosition >= outputLimit && !finalized) {
                    this.outputPosition = 0;
                    this.outputLimit = 0;

                    var plaintextRead = rawInputStream.read(plaintextBuffer, 0, plaintextBuffer.length);
                    if (plaintextRead == -1) {
                        rawInputStream.close();

                        var finalCiphertextLen = cipher.doFinal(ciphertextBuffer, 0);
                        processChunk(finalCiphertextLen);

                        var mac = ciphertextMac.doFinal();
                        // encFilehash digest covers ciphertext concatenated with the 10-byte HMAC trailer
                        ciphertextDigest.update(mac, 0, MAC_LENGTH);

                        var macSpace = outputBuffer.length - outputLimit;
                        var macToCopy = Math.min(MAC_LENGTH, macSpace);
                        System.arraycopy(mac, 0, outputBuffer, outputLimit, macToCopy);
                        outputLimit += macToCopy;

                        plaintextHash = plaintextDigest.digest();
                        ciphertextHash = ciphertextDigest.digest();

                        if (Log.DEBUG) {
                            LOGGER.log(Level.DEBUG, "encrypted media upload finished, plaintextLength={0}", plaintextLength);
                        }

                        finalized = true;
                        break;
                    }

                    plaintextDigest.update(plaintextBuffer, 0, plaintextRead);
                    plaintextLength += plaintextRead;

                    var ciphertextLen = cipher.update(plaintextBuffer, 0, plaintextRead, ciphertextBuffer, 0);
                    processChunk(ciphertextLen);
                }
            } catch (GeneralSecurityException exception) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "media encryption failed", exception);
                throw new IOException("Cannot encrypt data", exception);
            }
        }

        /**
         * Routes a freshly produced ciphertext chunk through the
         * {@code encFilehash} digest, the HMAC accumulator, and the
         * caller-facing output buffer.
         *
         * <p>Invoked by {@link #ensureDataAvailable()} after every cipher
         * update.
         *
         * @param length the number of valid ciphertext bytes in the cipher
         *               output buffer
         */
        @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "encryptAndHmac",
                adaptation = WhatsAppAdaptation.ADAPTED)
        private void processChunk(int length) {
            if (length <= 0) {
                return;
            }

            ciphertextDigest.update(ciphertextBuffer, 0, length);
            ciphertextMac.update(ciphertextBuffer, 0, length);

            var toCopy = Math.min(length, outputBuffer.length);
            System.arraycopy(ciphertextBuffer, 0, outputBuffer, 0, toCopy);
            outputLimit = toCopy;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public long fileLength() {
            return plaintextLength;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public byte[] fileSha256() {
            if (plaintextHash == null) {
                throw new IllegalStateException("Cannot get file SHA-256 hash before the file has been fully read");
            }

            return plaintextHash;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<byte[]> fileEncSha256() {
            if (ciphertextHash == null) {
                throw new IllegalStateException("Cannot get file encrypted SHA-256 hash before the file has been fully read");
            }

            return Optional.of(ciphertextHash);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns the random 32-byte media key
         * generated by the constructor; never empty.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<byte[]> fileKey() {
            return Optional.of(mediaKey);
        }
    }

    /**
     * A plaintext upload stream that passes the underlying content through
     * unchanged while computing the SHA-256 digest needed for the outgoing
     * message protobuf.
     *
     * <p>Used for media types that do not participate in end-to-end
     * encryption, such as newsletter media, profile pictures, and business
     * cover photos.
     */
    @WhatsAppWebModule(moduleName = "WAWebCryptoEncryptMedia")
    private static final class Plaintext extends MediaUploadInputStream {
        /**
         * Accumulates SHA-256 over the plaintext content.
         *
         * <p>Consumed by {@link #fileSha256()} once the underlying stream
         * is exhausted.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private final MessageDigest plaintextDigest;

        /**
         * The running count of plaintext bytes read from the underlying
         * stream.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private long plaintextLength;

        /**
         * The finalised SHA-256 digest of the plaintext.
         *
         * <p>Populated when {@link #read()} or
         * {@link #read(byte[], int, int)} observes end-of-stream, and
         * surfaced by {@link #fileSha256()}.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        private byte[] plaintextHash;

        /**
         * Tracks whether the underlying stream has been fully consumed and
         * the SHA-256 digest has been finalised.
         */
        private boolean finalized;

        /**
         * Constructs a plaintext upload stream.
         *
         * <p>Invoked from {@link #of(MediaProvider, InputStream)} for media
         * paths whose {@code keyName} is absent.
         *
         * @param rawInputStream the underlying input stream to pass through
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Plaintext(InputStream rawInputStream) {
            super(rawInputStream);
            try {
                this.plaintextDigest = MessageDigest.getInstance("SHA-256");
                this.plaintextLength = 0;
            } catch (GeneralSecurityException exception) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "plaintext media upload stream init failed", exception);
                throw new InternalError("Cannot initialize stream", exception);
            }
        }

        /**
         * Reads a single byte from the underlying stream while extending
         * the plaintext digest.
         *
         * <p>Finalises the plaintext digest the first time end-of-stream is
         * observed.
         *
         * @return the next byte of data, or {@code -1} if end of stream
         * @throws IOException if an I/O error occurs
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @Override
        public int read() throws IOException {
            var ch = rawInputStream.read();
            if (ch != -1) {
                plaintextDigest.update((byte) ch);
                plaintextLength++;
            } else if (!finalized) {
                finalized = true;
                plaintextHash = plaintextDigest.digest();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "plaintext media upload finished, plaintextLength={0}", plaintextLength);
            }
            return ch;
        }

        /**
         * Reads up to {@code len} bytes from the underlying stream while
         * extending the plaintext digest.
         *
         * <p>Finalises the plaintext digest the first time end-of-stream is
         * observed.
         *
         * @param b   the destination buffer
         * @param off the start offset in the destination buffer
         * @param len the maximum number of bytes to read
         * @return the number of bytes read, or {@code -1} if end of stream
         * @throws IOException if an I/O error occurs
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            var result = rawInputStream.read(b, off, len);
            if (result != -1) {
                plaintextDigest.update(b, off, result);
                plaintextLength += result;
            } else if (!finalized) {
                finalized = true;
                plaintextHash = plaintextDigest.digest();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "plaintext media upload finished, plaintextLength={0}", plaintextLength);
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public long fileLength() {
            return plaintextLength;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public byte[] fileSha256() {
            if (plaintextHash == null) {
                throw new IllegalStateException("Cannot get file SHA-256 hash before the file has been fully read");
            }

            return plaintextHash;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@link Optional#empty()}
         * because plaintext uploads never produce an encrypted hash.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<byte[]> fileEncSha256() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@link Optional#empty()}
         * because plaintext uploads never generate a media key.
         */
        @WhatsAppWebExport(moduleName = "WAWebCryptoEncryptMedia", exports = "default",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<byte[]> fileKey() {
            return Optional.empty();
        }
    }
}
