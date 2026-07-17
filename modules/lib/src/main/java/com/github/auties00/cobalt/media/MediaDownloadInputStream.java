package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Streams, decrypts, and verifies a media payload downloaded from
 * WhatsApp's CDN.
 *
 * <p>The wire format delivered by the CDN has the shape
 * {@code ciphertext || HMAC[0:10]}: the ciphertext is AES-CBC encrypted
 * and the trailing 10 bytes are the truncated HMAC-SHA256 over
 * {@code IV || ciphertext}. The IV itself is not present on the wire;
 * it is reproduced from the media key on the recipient side via HKDF.
 * Reads drive a four-state machine through {@link State#READ_DATA},
 * {@link State#READ_MAC}, {@link State#VALIDATE_ALL}, and
 * {@link State#DONE}. For inflatable media types (history-sync payloads)
 * the decrypted bytes are additionally decompressed with zlib before they
 * reach the caller.
 *
 * <p>Constructed by
 * {@link MediaConnectionService#tryDownload(MediaProvider, String)} after a
 * {@code 200 OK} from the CDN and surfaced to callers as the return value
 * of {@link MediaConnectionService#download(MediaProvider)}. The stream is
 * read like any {@link InputStream}; the cryptographic verification is
 * implicit and surfaces as {@link WhatsAppMediaException.Download} on any
 * failure.
 *
 * @implNote
 * This implementation is a streaming adapter over WA Web's batch
 * {@code hmacAndDecrypt}/{@code decryptMedia} pair: WA Web buffers the
 * full ciphertext in memory and decrypts it in one shot, whereas Cobalt
 * decrypts and verifies incrementally so large attachments can be piped
 * to the consumer without an in-memory copy. The wire-level outputs
 * (verification semantics, error classes) are identical.
 */
@WhatsAppWebModule(moduleName = "WAMediaCrypto")
@WhatsAppWebModule(moduleName = "WAWebCryptoDecryptMedia")
final class MediaDownloadInputStream extends MediaInputStream {
    /** The logger for {@link MediaDownloadInputStream}. */
    private static final System.Logger LOGGER = Log.get(MediaDownloadInputStream.class);

    /**
     * The HTTP client backing the download connection.
     *
     * <p>Owned by this stream and closed via {@link #close()} when the
     * caller releases the stream so that the underlying socket is released.
     */
    private final HttpClient client;

    /**
     * The zlib inflater for inflatable media types, or {@code null} when no
     * decompression is needed.
     *
     * <p>Only the {@code md-msg-hist} media type carries zlib-compressed
     * plaintext; {@code md-app-state} blobs are decoded directly from the
     * decrypted bytes.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsMediaTypes", exports = "MEDIA_TYPES",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private final Inflater inflater;

    /**
     * The primary working buffer used for reading ciphertext from the raw
     * stream and producing the in-place decrypted plaintext.
     *
     * @implNote
     * This implementation reuses the same buffer for both ciphertext input
     * and plaintext output because AES-CBC decryption supports in-place
     * substitution.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private final byte[] buffer;

    /**
     * The current read cursor within {@link #buffer}.
     */
    private int bufferOffset;

    /**
     * The number of valid decrypted bytes currently held by
     * {@link #buffer}.
     */
    private int bufferLimit;

    /**
     * The buffer holding decompressed output for inflatable media types,
     * or {@code null} when no decompression is needed.
     */
    private final byte[] inflatedBuffer;

    /**
     * The current read cursor within {@link #inflatedBuffer}.
     */
    private int inflatedOffset;

    /**
     * The number of valid decompressed bytes currently held by
     * {@link #inflatedBuffer}.
     */
    private int inflatedLimit;

    /**
     * The buffer accumulating the trailing HMAC bytes pulled from the raw
     * stream, or {@code null} for unencrypted media.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final byte[] macBuffer;

    /**
     * The number of HMAC bytes accumulated into {@link #macBuffer} so far.
     *
     * <p>Reaches {@link #MAC_LENGTH} once the trailer has been fully read
     * and the state machine can advance to {@link State#VALIDATE_ALL}.
     */
    private int macBufferOffset;

    /**
     * The SHA-256 digest accumulating the decrypted plaintext, or
     * {@code null} when no expected plaintext hash was supplied.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final MessageDigest plaintextDigest;

    /**
     * The expected SHA-256 of the plaintext used for integrity
     * verification, or {@code null} when no expected value was supplied.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final byte[] expectedPlaintextSha256;

    /**
     * The SHA-256 digest accumulating the encrypted payload
     * ({@code ciphertext || HMAC[0:10]}), or {@code null} when no expected
     * ciphertext hash was supplied.
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoDecryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final MessageDigest ciphertextDigest;

    /**
     * The expected SHA-256 of the encrypted payload used for integrity
     * verification, or {@code null} when no expected value was supplied.
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoDecryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final byte[] expectedCiphertextSha256;

    /**
     * The AES-CBC cipher in decrypt mode, or {@code null} when the media
     * type does not use end-to-end encryption.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final Cipher cipher;

    /**
     * The HMAC-SHA256 instance verifying the authenticity of the
     * downloaded payload, or {@code null} for unencrypted media.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final Mac mac;

    /**
     * The number of ciphertext bytes remaining before the HMAC trailer
     * begins.
     *
     * <p>Initialised to {@code payloadLength - MAC_LENGTH} on encrypted
     * media and to {@code payloadLength} on unencrypted media; decremented
     * as the raw stream is consumed and used by {@link #isDone()} to detect
     * the trailer boundary.
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoDecryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.DIRECT)
    private long remainingText;

    /**
     * The current state of the decryption and verification state machine.
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private State state;

    /**
     * Constructs a new download stream that transparently decrypts and
     * verifies the payload from the given raw stream.
     *
     * <p>Invoked from
     * {@link MediaConnectionService#tryDownload(MediaProvider, String)} once
     * the CDN has produced a {@code 200 OK} and the {@code Content-Length}
     * header has been read.
     *
     * @implNote
     * This implementation derives the IV, cipher key, and HMAC key via
     * HKDF when the provider advertises a media key, primes the HMAC with
     * the IV bytes so the tag covers {@code IV || ciphertext}, and
     * partitions the payload as
     * {@code payloadLength - MAC_LENGTH} ciphertext bytes followed by the
     * 10-byte HMAC trailer. When the provider has no media key the stream
     * passes through the raw bytes without decryption, still optionally
     * verifying the plaintext SHA-256 hash when one is supplied.
     *
     * @param client         the HTTP client managing the download
     * @param rawInputStream the raw input stream from the CDN
     * @param payloadLength  the total payload length in bytes (ciphertext
     *                       plus HMAC trailer)
     * @param provider       the media provider with decryption metadata
     * @throws WhatsAppMediaException if key derivation or cipher
     *         initialisation fails, or if exactly one of media key and key
     *         name is present
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCryptoDecryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    MediaDownloadInputStream(HttpClient client, InputStream rawInputStream, long payloadLength, MediaProvider provider) throws WhatsAppMediaException {
        super(rawInputStream);
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
        Objects.requireNonNull(provider, "provider must not be null");

        this.client = client;

        this.inflater = provider.mediaPath().inflatable() ? new Inflater() : null;

        this.buffer = new byte[BUFFER_LENGTH];
        this.inflatedBuffer = isInflatable() ? new byte[BUFFER_LENGTH] : null;

        this.expectedPlaintextSha256 = provider.mediaSha256().orElse(null);
        this.plaintextDigest = expectedPlaintextSha256 != null ? newHash() : null;

        var hasKeyName = provider.mediaPath().keyName().isPresent();
        var hasMediaKey = provider.mediaKey().isPresent();

        if (hasKeyName != hasMediaKey) {
            throw new WhatsAppMediaException.Download("Media key and key name must both be present or both be absent");
        } else if (hasKeyName) {
            this.expectedCiphertextSha256 = provider.mediaEncryptedSha256().orElse(null);
            this.ciphertextDigest = expectedCiphertextSha256 != null ? newHash() : null;

            var mediaKey = provider.mediaKey()
                    .orElseThrow(() -> new WhatsAppMediaException.Download("Media key must be present"));
            var keyName = provider.mediaPath().keyName()
                    .orElseThrow(() -> new WhatsAppMediaException.Download("Key name must be present"));

            var expanded = deriveMediaKeyData(mediaKey, keyName);
            var iv = new IvParameterSpec(expanded, 0, IV_LENGTH);
            var cipherKey = new SecretKeySpec(expanded, IV_LENGTH, KEY_LENGTH, "AES");
            var macKey = new SecretKeySpec(expanded, IV_LENGTH + KEY_LENGTH, KEY_LENGTH, "HmacSHA256");

            this.cipher = newCipher(Cipher.DECRYPT_MODE, cipherKey, iv);
            this.mac = newMac(macKey);

            this.mac.update(expanded, 0, IV_LENGTH);

            this.remainingText = payloadLength - MAC_LENGTH;
            this.macBuffer = new byte[MAC_LENGTH];
        } else {
            this.expectedCiphertextSha256 = null;
            this.ciphertextDigest = null;
            this.cipher = null;
            this.mac = null;
            this.macBuffer = null;
            this.remainingText = payloadLength;
        }

        this.state = State.READ_DATA;

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "media download stream opened: encrypted={0} inflatable={1} payloadLength={2}",
                    hasMediaKey, isInflatable(), payloadLength);
        }
    }

    /**
     * Reads a single decrypted (and optionally decompressed) byte.
     *
     * <p>Drives the state machine through {@link #isDone()} on each
     * invocation until either a byte is available or the stream is fully
     * consumed and validated.
     *
     * @return the next byte of decrypted data, or {@code -1} if the stream
     *         is exhausted and validated
     * @throws WhatsAppMediaException.Download if decryption, decompression,
     *         or validation fails
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public int read() throws WhatsAppMediaException.Download {
        if (isDone()) {
            return -1;
        } else if (isInflatable()) {
            return inflatedBuffer[inflatedOffset++] & 0xFF;
        } else {
            return buffer[bufferOffset++] & 0xFF;
        }
    }

    /**
     * Reads up to {@code len} decrypted (and optionally decompressed) bytes
     * into the supplied array.
     *
     * <p>Drives the state machine through {@link #isDone()} on each
     * invocation; returns as soon as the next decrypted chunk is staged, up
     * to {@code len} bytes.
     *
     * @param b   the destination buffer
     * @param off the start offset in the destination buffer
     * @param len the maximum number of bytes to read
     * @return the number of bytes read, or {@code -1} if the stream is
     *         exhausted and validated
     * @throws WhatsAppMediaException.Download if decryption, decompression,
     *         or validation fails
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public int read(byte[] b, int off, int len) throws WhatsAppMediaException.Download {
        if (isDone()) {
            return -1;
        } else if (isInflatable()) {
            var toRead = Math.min(len, inflatedLimit - inflatedOffset);
            System.arraycopy(inflatedBuffer, inflatedOffset, b, off, toRead);
            inflatedOffset += toRead;
            return toRead;
        } else {
            var toRead = Math.min(len, bufferLimit - bufferOffset);
            System.arraycopy(buffer, bufferOffset, b, off, toRead);
            bufferOffset += toRead;
            return toRead;
        }
    }

    /**
     * Drives the decryption and verification state machine forward until
     * output data becomes available or the stream is fully consumed and
     * validated.
     *
     * <p>Internal pump for {@link #read()} and
     * {@link #read(byte[], int, int)}; reads keep blocking on
     * {@link #rawInputStream} until either a chunk of decrypted (or
     * inflated) bytes is staged or the validation tail completes.
     *
     * @implNote
     * This implementation collapses WA Web's four functions
     * ({@code mms4Download}, {@code hmacAndDecrypt}, {@code decryptMedia},
     * and the inflation pass on history-sync payloads) into a
     * single state machine driven by {@link State}, so the whole pipeline
     * runs incrementally on one virtual thread without buffering the
     * payload in memory.
     *
     * @return {@code true} if the stream is fully consumed and validated,
     *         {@code false} if output data is available for reading
     * @throws WhatsAppMediaException.Download if any decryption or
     *         validation step fails
     */
    @WhatsAppWebExport(moduleName = "WAMediaCrypto", exports = "hmacAndDecrypt",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCryptoDecryptMedia", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isDone() throws WhatsAppMediaException.Download {
        try {
            var inflatable = isInflatable();
            while ((inflatable ? inflatedOffset >= inflatedLimit : bufferOffset >= bufferLimit) && state != State.DONE) {
                if (inflatable && !inflater.needsInput() && !inflater.finished()) {
                    inflatedOffset = 0;
                    inflatedLimit = inflater.inflate(inflatedBuffer);
                } else {
                    switch (state) {
                        case READ_DATA -> {
                            if (remainingText > 0) {
                                var toRead = (int) Math.min(buffer.length, remainingText);
                                var read = rawInputStream.read(buffer, 0, toRead);
                                if (read == -1) {
                                    if (Log.WARNING) {
                                        LOGGER.log(Level.WARNING, "download stream ended early: expected {0} more bytes", remainingText);
                                    }
                                    throw new WhatsAppMediaException.Download("Unexpected end of stream: expected " + remainingText + " more bytes");
                                }
                                remainingText -= read;

                                if (isEncrypted()) {
                                    if (ciphertextDigest != null) {
                                        ciphertextDigest.update(buffer, 0, read);
                                    }

                                    mac.update(buffer, 0, read);

                                    bufferOffset = 0;
                                    bufferLimit = cipher.update(buffer, 0, read, buffer, 0);
                                } else {
                                    bufferOffset = 0;
                                    bufferLimit = read;
                                }

                                if (plaintextDigest != null) {
                                    plaintextDigest.update(buffer, 0, bufferLimit);
                                }

                                if (inflatable) {
                                    inflater.setInput(buffer, 0, bufferLimit);

                                    inflatedOffset = 0;
                                    inflatedLimit = inflater.inflate(inflatedBuffer);
                                }
                            } else {
                                if (isEncrypted()) {
                                    bufferOffset = 0;
                                    bufferLimit = cipher.doFinal(buffer, 0);

                                    if (plaintextDigest != null) {
                                        plaintextDigest.update(buffer, 0, bufferLimit);
                                    }

                                    if (inflatable) {
                                        inflater.setInput(buffer, 0, bufferLimit);

                                        inflatedOffset = 0;
                                        inflatedLimit = inflater.inflate(inflatedBuffer);
                                    }

                                    state = State.READ_MAC;
                                } else {
                                    if (!inflatable || inflater.finished()) {
                                        state = State.VALIDATE_ALL;
                                    }
                                }
                            }
                        }

                        case READ_MAC -> {
                            var toRead = MAC_LENGTH - macBufferOffset;
                            if (toRead > 0) {
                                var read = rawInputStream.read(macBuffer, macBufferOffset, toRead);
                                if (read == -1) {
                                    if (Log.WARNING) {
                                        LOGGER.log(Level.WARNING, "download stream ended early while reading mac: expected {0} more bytes", toRead);
                                    }
                                    throw new WhatsAppMediaException.Download("Unexpected end of stream: expected " + toRead + " more bytes");
                                }
                                macBufferOffset += read;
                            }

                            if (macBufferOffset == MAC_LENGTH) {
                                if (ciphertextDigest != null) {
                                    // encFilehash covers ciphertext concatenated with the 10-byte HMAC trailer
                                    ciphertextDigest.update(macBuffer);
                                }

                                if (!inflatable || inflater.finished()) {
                                    state = State.VALIDATE_ALL;
                                }
                            }
                        }

                        case VALIDATE_ALL -> {
                            if (isEncrypted()) {
                                if (ciphertextDigest != null) {
                                    var actualCiphertextSha256 = ciphertextDigest.digest();
                                    if (!MessageDigest.isEqual(expectedCiphertextSha256, actualCiphertextSha256)) {
                                        if (Log.ERROR) LOGGER.log(Level.ERROR, "download ciphertext hash mismatch");
                                        throw new WhatsAppMediaException.Download("Ciphertext SHA256 hash doesn't match the expected value");
                                    }
                                }

                                var actualCiphertextMac = mac.doFinal();
                                if (!MessageDigest.isEqual(
                                        Arrays.copyOf(macBuffer, MAC_LENGTH),
                                        Arrays.copyOf(actualCiphertextMac, MAC_LENGTH))) {
                                    if (Log.ERROR) LOGGER.log(Level.ERROR, "download mac verification failed");
                                    throw new WhatsAppMediaException.Download("Mac doesn't match the expected value");
                                }
                            }

                            if (plaintextDigest != null) {
                                var actualPlaintextSha256 = plaintextDigest.digest();
                                if (!MessageDigest.isEqual(expectedPlaintextSha256, actualPlaintextSha256)) {
                                    if (Log.ERROR) LOGGER.log(Level.ERROR, "download plaintext hash mismatch");
                                    throw new WhatsAppMediaException.Download("Plaintext SHA256 hash doesn't match the expected value");
                                }
                            }

                            state = State.DONE;
                        }
                    }
                }
            }

            return state == State.DONE;
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "download read failed", exception);
            throw new WhatsAppMediaException.Download("Cannot read data", exception);
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "download decrypt failed", exception);
            throw new WhatsAppMediaException.Download("Cannot decrypt data", exception);
        } catch (DataFormatException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "download inflate failed", exception);
            throw new WhatsAppMediaException.Download("Cannot inflate data", exception);
        }
    }

    /**
     * Tests whether this stream is processing encrypted media.
     *
     * <p>Toggles between the ciphertext-and-HMAC fast path and the raw
     * pass-through path inside {@link #isDone()}.
     *
     * @return {@code true} if the cipher is initialised, {@code false} for
     *         unencrypted media
     */
    private boolean isEncrypted() {
        return cipher != null;
    }

    /**
     * Tests whether this stream applies zlib decompression after
     * decryption.
     *
     * <p>Toggles the inflate branch of the state machine for the
     * {@code md-msg-hist} media type.
     *
     * @return {@code true} if the inflater is initialised, {@code false}
     *         otherwise
     */
    private boolean isInflatable() {
        return inflater != null;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation closes the underlying raw stream via
     * {@code super.close()}, releases the owned {@link HttpClient}, and
     * frees the zlib inflater when one was allocated.
     */
    @Override
    public void close() throws IOException {
        super.close();
        client.close();
        if (inflater != null) {
            inflater.end();
        }
    }

    /**
     * The explicit states of the decryption and verification state
     * machine driven by {@link #isDone()}.
     */
    private enum State {
        /**
         * Reads ciphertext from the raw stream, decrypts it, and
         * optionally inflates the result for the caller.
         */
        READ_DATA,

        /**
         * Pulls the trailing 10-byte HMAC from the raw stream.
         */
        READ_MAC,

        /**
         * Verifies the encrypted-payload hash, the HMAC, and the plaintext
         * hash.
         */
        VALIDATE_ALL,

        /**
         * Marks the stream as fully consumed and validated.
         */
        DONE
    }
}
