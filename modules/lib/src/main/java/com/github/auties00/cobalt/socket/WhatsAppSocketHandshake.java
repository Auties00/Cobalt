package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.HandshakeMessage;
import com.github.auties00.cobalt.wire.linked.device.pairing.HandshakeMessageSpec;
import com.github.auties00.cobalt.socket.datagram.WhatsAppDatagramInputStream;
import com.github.auties00.cobalt.socket.datagram.WhatsAppDatagramOutputStream;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Objects;

/**
 * Cryptographic state machine for the WhatsApp Noise XX key exchange
 * plus the I/O dispatch for the three handshake messages.
 *
 * <p>The handshake chains a SHA-256 hash, an HKDF salt, a symmetric
 * cipher key and a 64-bit nonce counter. Each
 * {@link #cipher(byte[], boolean)} call encrypts or decrypts a
 * payload, mixes the resulting bytes into the running hash and
 * advances the nonce. {@link #mixIntoKey(byte[])} folds new key
 * material into the salt and cipher key whenever a Diffie-Hellman
 * shared secret has been computed.
 *
 * <p>I/O is dispatched through the {@link WhatsAppDatagramInputStream}
 * and {@link WhatsAppDatagramOutputStream} passed at construction.
 * {@link #writeClientHandshake(HandshakeMessage)} serialises a
 * {@link HandshakeMessage} into the output stream (prepending the
 * Noise prologue on the first call so the {@code "WA\x06\x07"} or
 * {@code "WA\x05\x07"} header rides inside the first WebSocket
 * frame), and {@link #readServerHandshake()} reads one wire datagram
 * back into a {@link HandshakeMessage}.
 *
 * <p>{@link #finish()} derives the final 64 bytes of key material
 * (32-byte write key followed by 32-byte read key) once both sides
 * have exchanged their {@code ClientFinish}/{@code ServerHello}
 * messages.
 *
 * <p>Instances are {@link AutoCloseable} and the WhatsApp socket
 * client always uses them inside a try-with-resources block so that
 * the AES key is destroyed promptly when the handshake completes or
 * fails.
 *
 * @implNote
 * This implementation collapses WA Web's {@code constructor} and
 * {@code start} steps into one constructor and reads handshake
 * messages through a bounded anonymous {@link InputStream} sized off
 * {@link WhatsAppDatagramInputStream#readDatagramLength()} so the
 * {@link ProtobufInputStream} stops cleanly at the datagram boundary
 * without consuming bytes from the next datagram.
 */
@WhatsAppWebModule(moduleName = "WANoiseHandshake")
final class WhatsAppSocketHandshake implements AutoCloseable {

    /**
     * The logger for {@link WhatsAppSocketHandshake}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppSocketHandshake.class);

    /**
     * The empty IKM passed to HKDF when {@link #finish()} expands the
     * final read and write keys, mirroring WA Web's
     * {@code new Uint8Array(0)}.
     */
    private static final byte[] FINISH_KEY = new byte[0];

    /**
     * The Noise protocol name padded to exactly 32 bytes so it can be
     * used verbatim as the initial hash, salt and cipher key without
     * an additional SHA-256 reduction.
     */
    private static final byte[] NOISE_PROTOCOL = "Noise_XX_25519_AESGCM_SHA256\0\0\0\0".getBytes(StandardCharsets.UTF_8);

    /**
     * The input stream supplying the wire bytes the server sends
     * during the handshake.
     *
     * <p>The handshake runs while the stream is in pre-handshake passthrough
     * mode (no decryption); once the keys are derived the caller installs
     * them via {@link WhatsAppDatagramInputStream#setReadKey(SecretKey)}.
     */
    private final WhatsAppDatagramInputStream inputStream;

    /**
     * The output stream that frames each outbound handshake message
     * into one wire datagram.
     *
     * <p>The first {@link #writeClientHandshake(HandshakeMessage)} call
     * prepends {@link #prologue} so the prologue and the {@code ClientHello}
     * ride inside the same WebSocket frame on the browser client.
     */
    private final WhatsAppDatagramOutputStream outputStream;

    /**
     * The Noise protocol prologue (the WhatsApp version header, for
     * example {@code "WA\x06\x07"}) prepended to the first outbound
     * handshake message; passed to
     * {@link WhatsAppDatagramOutputStream#beginDatagram(byte[], int)}
     * on the first {@link #writeClientHandshake(HandshakeMessage)}
     * call and replaced by {@code null} on every subsequent call.
     */
    private final byte[] prologue;

    /**
     * Whether the prologue has already been emitted on the output
     * stream.
     */
    private boolean prologueSent;

    /**
     * The HKDF-SHA256 instance reused for every salt rotation.
     */
    private final KDF kdf;

    /**
     * The SHA-256 digest reused to chain
     * {@code SHA-256(hash || data)} into the running handshake hash.
     */
    private final MessageDigest hashDigest;

    /**
     * The AES/GCM/NoPadding cipher reused to encrypt and decrypt
     * every handshake payload.
     */
    private final Cipher cipher;

    /**
     * The running handshake hash; starts at {@link #NOISE_PROTOCOL}
     * and is updated on every {@code authenticate} step.
     */
    private byte[] hash;

    /**
     * The current HKDF salt; rotated by {@link #mixIntoKey(byte[])}
     * whenever fresh shared key material is folded in.
     */
    private SecretKeySpec salt;

    /**
     * The current symmetric AES key used as the AAD-bearing payload
     * cipher.
     */
    private SecretKeySpec cryptoKey;

    /**
     * The AES-GCM nonce counter; reset to zero whenever the cipher
     * key rotates.
     */
    private long counter;

    /**
     * Initializes the handshake state, captures the I/O streams and
     * mixes the prologue into the running hash.
     *
     * <p>One instance is constructed per WhatsApp socket connect, inside a
     * try-with-resources block in
     * {@link WhatsAppSocketClient#performNoiseHandshake()}, so the AES key is
     * destroyed promptly when the handshake completes or fails.
     *
     * @implNote
     * This implementation merges WA Web's {@code constructor} and
     * {@code start} steps. The hash, salt and cipher key all begin
     * as the 32-byte Noise protocol name, then the prologue (the
     * WhatsApp version header) is folded into the running hash via
     * {@link #updateHash(byte[])}.
     *
     * @param inputStream  the datagram input stream the handshake
     *                     reads server messages from
     * @param outputStream the datagram output stream the handshake
     *                     writes client messages to
     * @param prologue     the protocol prologue bytes that identify
     *                     the client variant (web or mobile)
     * @throws NoSuchAlgorithmException if HKDF-SHA256 or SHA-256 is
     *         unavailable on this JDK
     * @throws NoSuchPaddingException   if AES/GCM/NoPadding is
     *         unavailable on this JDK
     */
    @WhatsAppWebExport(moduleName = "WANoiseHandshake", exports = "NoiseHandshake", adaptation = WhatsAppAdaptation.ADAPTED)
    WhatsAppSocketHandshake(WhatsAppDatagramInputStream inputStream,
                            WhatsAppDatagramOutputStream outputStream,
                            byte[] prologue) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.inputStream = Objects.requireNonNull(inputStream, "inputStream");
        this.outputStream = Objects.requireNonNull(outputStream, "outputStream");
        this.prologue = Objects.requireNonNull(prologue, "prologue");
        this.kdf = KDF.getInstance("HKDF-SHA256");
        this.hashDigest = MessageDigest.getInstance("SHA-256");
        this.cipher = Cipher.getInstance("AES/GCM/NoPadding");
        this.hash = NOISE_PROTOCOL;
        this.salt = new SecretKeySpec(NOISE_PROTOCOL, "AES");
        this.cryptoKey = new SecretKeySpec(NOISE_PROTOCOL, 0, 32, "AES");
        this.counter = 0;
        updateHash(prologue);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "noise handshake initialized, prologue={0}", prologue);
    }

    /**
     * Serialises the supplied client-side {@link HandshakeMessage}
     * (one of {@code ClientHello} or {@code ClientFinish}) and emits
     * it as one wire datagram on the underlying output stream.
     *
     * <p>The first call passes the Noise prologue to
     * {@link WhatsAppDatagramOutputStream#beginDatagram(byte[], int)} so the
     * prologue and the {@code ClientHello} ride inside the same WebSocket
     * frame on the browser client; subsequent calls pass {@code null} as the
     * prologue argument so only the {@code int24}-framed handshake bytes hit
     * the wire.
     *
     * @param message the handshake message to send
     * @throws IOException if the underlying write fails
     */
    @WhatsAppWebExport(moduleName = "WANoiseHandshake", exports = "NoiseHandshake", adaptation = WhatsAppAdaptation.ADAPTED)
    void writeClientHandshake(HandshakeMessage message) throws IOException {
        Objects.requireNonNull(message, "message");
        var firstPrologue = prologueSent ? null : prologue;
        prologueSent = true;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "writing client handshake message, prologue={0}", firstPrologue != null);
        outputStream.beginDatagram(firstPrologue, HandshakeMessageSpec.sizeOf(message));
        HandshakeMessageSpec.encode(message, ProtobufOutputStream.toStream(outputStream));
        outputStream.flush();
    }

    /**
     * Reads exactly one wire datagram from the underlying input
     * stream and decodes its payload as a {@link HandshakeMessage}.
     *
     * <p>This drains the server's {@code ServerHello} between the client's
     * {@code ClientHello} and {@code ClientFinish}; a second call would
     * attempt to read a fourth handshake message that the protocol does not
     * define.
     *
     * @implNote
     * This implementation builds a bounded anonymous
     * {@link InputStream} sized off
     * {@link WhatsAppDatagramInputStream#readDatagramLength()} so the
     * {@link ProtobufInputStream} stops cleanly at the datagram
     * boundary without consuming bytes from the next datagram.
     *
     * @return the parsed handshake message
     * @throws IOException if the server closes the connection, the
     *                     length prefix is invalid, or the protobuf
     *                     payload is malformed
     */
    @WhatsAppWebExport(moduleName = "WANoiseHandshake", exports = "NoiseHandshake", adaptation = WhatsAppAdaptation.ADAPTED)
    HandshakeMessage readServerHandshake() throws IOException {
        var plaintextLength = inputStream.readDatagramLength();
        if (plaintextLength < 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "server closed connection before next handshake message");
            throw new IOException("Server closed the connection before sending the next Noise handshake message");
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "reading server handshake message, length={0}", plaintextLength);
        var bounded = new InputStream() {
            private int remaining = plaintextLength;

            @Override
            public int read() throws IOException {
                if (remaining <= 0) {
                    return -1;
                }
                var b = inputStream.read();
                if (b < 0) {
                    throw new IOException("Unexpected end of stream mid-handshake (expected "
                            + remaining + " more byte(s))");
                }
                remaining--;
                return b;
            }

            @Override
            public int read(byte[] buf, int off, int len) throws IOException {
                if (remaining <= 0) {
                    return -1;
                }
                if (len == 0) {
                    return 0;
                }
                var toRead = Math.min(len, remaining);
                var n = inputStream.read(buf, off, toRead);
                if (n < 0) {
                    throw new IOException("Unexpected end of stream mid-handshake (expected "
                            + remaining + " more byte(s))");
                }
                remaining -= n;
                return n;
            }
        };
        return HandshakeMessageSpec.decode(ProtobufInputStream.fromStream(bounded));
    }

    /**
     * Folds {@code data} into the running hash by computing
     * {@code SHA-256(hash || data)} and storing the digest as the
     * new hash.
     *
     * <p>Each {@code authenticate} step in WA Web's Noise handshake
     * corresponds to one call here; the running hash binds every handshake
     * message (and the prologue) so the final AES keys cannot be replayed
     * against a different handshake transcript.
     *
     * @param data the bytes to mix into the running hash
     */
    @WhatsAppWebExport(moduleName = "WANoiseHandshake", exports = "NoiseHandshake", adaptation = WhatsAppAdaptation.DIRECT)
    public void updateHash(byte[] data) {
        hashDigest.update(hash);
        hashDigest.update(data);
        this.hash = hashDigest.digest();
    }

    /**
     * Encrypts or decrypts a payload with the current AES key, using
     * the running hash as AES-GCM AAD and the monotonic nonce
     * counter.
     *
     * <p>Encryption mixes the produced ciphertext into the running hash;
     * decryption mixes the input ciphertext. The 64-bit nonce counter is
     * appended big-endian to a 4-byte zero prefix to form the 12-byte GCM
     * IV.
     *
     * @param text    plaintext when {@code encrypt} is {@code true},
     *                ciphertext when {@code false}
     * @param encrypt {@code true} to encrypt, {@code false} to
     *                decrypt
     * @return the ciphertext or plaintext respectively
     * @throws IllegalBlockSizeException          if the input length
     *                                            is invalid
     * @throws BadPaddingException                if the GCM
     *                                            authentication tag
     *                                            fails
     * @throws InvalidAlgorithmParameterException if the nonce
     *                                            parameters are
     *                                            invalid
     * @throws InvalidKeyException                if the cipher key
     *                                            is invalid
     */
    @WhatsAppWebExport(moduleName = "WANoiseHandshake", exports = "NoiseHandshake", adaptation = WhatsAppAdaptation.DIRECT)
    byte[] cipher(byte[] text, boolean encrypt) throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        var nonce = new byte[12];
        DataUtils.putLong(nonce, 4, counter++, ByteOrder.BIG_ENDIAN);
        cipher.init(
                encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                cryptoKey,
                new GCMParameterSpec(128, nonce)
        );
        cipher.updateAAD(hash);
        var result = cipher.doFinal(text);
        updateHash(encrypt ? result : text);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "handshake cipher step encrypt={0} inLen={1} outLen={2}", encrypt, text.length, result.length);
        return result;
    }

    /**
     * Derives the final 64 bytes of key material from the current
     * salt via HKDF with an empty IKM.
     *
     * <p>The first 32 bytes are the write key and the remaining 32 bytes are
     * the read key. WA Web wraps the same material in a {@code NoiseSocket};
     * Cobalt returns the raw bytes so the caller can install them on the
     * datagram streams directly via
     * {@link WhatsAppDatagramOutputStream#setWriteKey(SecretKey)} and
     * {@link WhatsAppDatagramInputStream#setReadKey(SecretKey)}.
     *
     * @return the concatenated write and read key material
     * @throws GeneralSecurityException if HKDF expansion fails
     */
    @WhatsAppWebExport(moduleName = "WANoiseHandshake", exports = "NoiseHandshake", adaptation = WhatsAppAdaptation.ADAPTED)
    byte[] finish() throws GeneralSecurityException {
        var params = HKDFParameterSpec.ofExtract()
                .addSalt(salt)
                .addIKM(FINISH_KEY)
                .thenExpand(null, 64);
        var derived = kdf.deriveData(params);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "noise handshake key material derived");
        return derived;
    }

    /**
     * Folds new key material into the handshake state.
     *
     * <p>This is called after each Diffie-Hellman shared secret is computed
     * during the handshake; rotating the salt and cipher key after every DH
     * operation is what gives Noise XX its forward-secrecy guarantees.
     *
     * @implNote
     * This implementation runs HKDF-Extract-and-Expand under the
     * current salt to produce 64 bytes: the first 32 bytes become
     * the new salt, the remaining 32 bytes become the new cipher
     * key, and the nonce counter is reset to zero so the rotated
     * key starts at nonce 0.
     *
     * @param bytes the new key material, typically a Curve25519
     *              shared secret produced during the handshake
     * @throws GeneralSecurityException if HKDF expansion fails
     */
    @WhatsAppWebExport(moduleName = "WANoiseHandshake", exports = "NoiseHandshake", adaptation = WhatsAppAdaptation.DIRECT)
    void mixIntoKey(byte[] bytes) throws GeneralSecurityException {
        var params = HKDFParameterSpec.ofExtract()
                .addSalt(salt)
                .addIKM(bytes)
                .thenExpand(null, 64);
        var expanded = kdf.deriveData(params);
        this.salt = new SecretKeySpec(expanded, 0, 32, "AES");
        this.cryptoKey = new SecretKeySpec(expanded, 32, 32, "AES");
        this.counter = 0;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "handshake key material mixed, salt and cipher key rotated");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation drops the hash and salt references,
     * attempts to destroy the cipher key via
     * {@link SecretKeySpec#destroy()} (silently ignoring
     * implementations that do not support destruction) and resets
     * the nonce counter. WA Web relies on the browser's garbage
     * collector for the same effect; Cobalt does it eagerly so
     * secrets do not linger in heap memory for longer than the
     * handshake.
     */
    @Override
    public void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing noise handshake state");
        this.hash = null;
        this.salt = null;
        try {
            cryptoKey.destroy();
        } catch (DestroyFailedException _) {

        }
        this.cryptoKey = null;
        this.counter = 0;
    }
}
