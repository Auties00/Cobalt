package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.exception.MediaException;
import com.github.auties00.cobalt.model.media.MediaProvider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class MediaDownloadInputStream extends MediaInputStream {
    private final HttpClient client;
    private final Inflater inflater;

    private final byte[] buffer;
    private int bufferOffset;
    private int bufferLimit;

    private final byte[] inflatedBuffer;
    private int inflatedOffset;
    private int inflatedLimit;

    private final byte[] macBuffer;
    private int macBufferOffset;

    private final MessageDigest plaintextDigest;
    private final byte[] expectedPlaintextSha256;

    private final MessageDigest ciphertextDigest;
    private final byte[] expectedCiphertextSha256;

    private final Cipher cipher;

    private final Mac mac;

    private long remainingText;

    private State state;

    MediaDownloadInputStream(HttpClient client, InputStream rawInputStream, long payloadLength, MediaProvider provider) throws MediaException {
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
            throw new MediaDownloadException("Media key and key name must both be present or both be absent");
        } else if (hasKeyName) {
            this.expectedCiphertextSha256 = provider.mediaEncryptedSha256().orElse(null);
            this.ciphertextDigest = expectedCiphertextSha256 != null ? newHash() : null;

            var mediaKey = provider.mediaKey()
                    .orElseThrow(() -> new MediaDownloadException("Media key must be present"));
            var keyName = provider.mediaPath().keyName()
                    .orElseThrow(() -> new MediaDownloadException("Key name must be present"));

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
    }

    @Override
    public int read() throws MediaDownloadException {
        if (isDone()) {
            return -1;
        } else if (isInflatable()) {
            return inflatedBuffer[inflatedOffset++] & 0xFF;
        } else {
            return buffer[bufferOffset++] & 0xFF;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws MediaDownloadException {
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


    private boolean isDone() throws MediaDownloadException {
        try {
            var inflatable = isInflatable();
            while ((inflatable ? inflatedOffset >= inflatedLimit : bufferOffset >= bufferLimit) && state != State.DONE) {
                if(inflatable && !inflater.needsInput() && !inflater.finished()) {
                    inflatedOffset = 0;
                    inflatedLimit = inflater.inflate(inflatedBuffer);
                }else {
                    switch (state) {
                        case READ_DATA -> {
                            if (remainingText > 0) {
                                var toRead = (int) Math.min(buffer.length, remainingText);
                                var read = rawInputStream.read(buffer, 0, toRead);
                                if (read == -1) {
                                    throw new MediaDownloadException("Unexpected end of stream: expected " + remainingText + " more bytes");
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

                                if(plaintextDigest != null) {
                                    plaintextDigest.update(buffer, 0, bufferLimit);
                                }

                                if(inflatable) {
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

                                    if(inflatable) {
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
                            if(toRead > 0) {
                                var read = rawInputStream.read(macBuffer, macBufferOffset, toRead);
                                if (read == -1) {
                                    throw new MediaDownloadException("Unexpected end of stream: expected " + toRead + " more bytes");
                                }
                                macBufferOffset += read;
                            }

                            if (macBufferOffset == MAC_LENGTH) {
                                if (ciphertextDigest != null) {
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
                                    if (!Arrays.equals(expectedCiphertextSha256, actualCiphertextSha256)) {
                                        throw new MediaDownloadException("Ciphertext SHA256 hash doesn't match the expected value");
                                    }
                                }

                                var actualCiphertextMac = mac.doFinal();
                                if (!Arrays.equals(macBuffer, 0, MAC_LENGTH, actualCiphertextMac, 0, MAC_LENGTH)) {
                                    throw new MediaDownloadException("Mac doesn't match the expected value");
                                }
                            }

                            if (plaintextDigest != null) {
                                var actualPlaintextSha256 = plaintextDigest.digest();
                                if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                                    throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
                                }
                            }

                            state = State.DONE;
                        }
                    }
                }
            }

            return state == State.DONE;
        } catch (IOException exception) {
            throw new MediaDownloadException("Cannot read data", exception);
        } catch (GeneralSecurityException exception) {
            throw new MediaDownloadException("Cannot decrypt data", exception);
        } catch (DataFormatException exception) {
            throw new MediaDownloadException("Cannot inflate data", exception);
        }
    }

    private boolean isEncrypted() {
        return cipher != null;
    }

    private boolean isInflatable() {
        return inflater != null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        client.close();
        if (inflater != null) {
            inflater.end();
        }
    }

    private enum State {
        READ_DATA,
        READ_MAC,
        VALIDATE_ALL,
        DONE
    }
}