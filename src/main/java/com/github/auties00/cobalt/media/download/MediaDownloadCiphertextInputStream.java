package com.github.auties00.cobalt.media.download;

import com.github.auties00.cobalt.exception.HmacValidationException;
import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.model.media.MutableAttachmentProvider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

public final class MediaDownloadCiphertextInputStream extends MediaDownloadInputStream {
    private final byte[] ciphertextBuffer;
    private final byte[] plaintextBuffer;
    private final MessageDigest plaintextDigest;
    private final MessageDigest ciphertextDigest;
    private final Mac mac;
    private final Cipher cipher;
    private final byte[] expectedPlaintextSha256;
    private final byte[] expectedCiphertextSha256;

    private int bufferOffset = 0;
    private int bufferLimit = 0;
    private long remainingCiphertext;
    private boolean finished = false;
    private boolean validated = false;

    public MediaDownloadCiphertextInputStream(InputStream rawInputStream, int payloadLength, MutableAttachmentProvider provider) throws GeneralSecurityException {
        Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
        Objects.requireNonNull(provider, "provider must not be null");

        super(rawInputStream, provider.attachmentType().inflatable());
        this.ciphertextBuffer = new byte[BUFFER_LENGTH];
        this.plaintextBuffer = new byte[BUFFER_LENGTH];
        this.expectedPlaintextSha256 = provider.mediaSha256().orElse(null);
        this.plaintextDigest = expectedPlaintextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;

        this.expectedCiphertextSha256 = provider.mediaEncryptedSha256().orElse(null);
        this.ciphertextDigest = expectedCiphertextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;

        var mediaKey = provider.mediaKey()
                .orElseThrow(() -> new MediaDownloadException("Media key must be present"));
        var keyName = provider.attachmentType()
                .keyName()
                .orElseThrow(() -> new MediaDownloadException("Key name must be present"));
        var expanded = deriveMediaKeyData(mediaKey, keyName);
        var iv = new IvParameterSpec(expanded, 0, IV_LENGTH);
        var cipherKey = new SecretKeySpec(expanded, IV_LENGTH, KEY_LENGTH, "AES");
        var macKey = new SecretKeySpec(expanded, IV_LENGTH + KEY_LENGTH, KEY_LENGTH, "HmacSHA256");

        this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        this.cipher.init(Cipher.DECRYPT_MODE, cipherKey, iv);

        this.mac = Mac.getInstance("HmacSHA256");
        this.mac.init(macKey);
        this.mac.update(expanded, 0, IV_LENGTH);

        this.remainingCiphertext = payloadLength - MAC_LENGTH;
    }

    @Override
    public int read() throws IOException {
        if (ensureDataAvailable()) {
            return -1;
        }
        return plaintextBuffer[bufferOffset++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (ensureDataAvailable()) {
            return -1;
        }

        var available = bufferLimit - bufferOffset;
        var toRead = Math.min(len, available);
        System.arraycopy(plaintextBuffer, bufferOffset, b, off, toRead);
        bufferOffset += toRead;
        return toRead;
    }

    private boolean ensureDataAvailable() throws IOException {
        try {
            while (bufferOffset >= bufferLimit && !finished) {
                if (remainingCiphertext == 0) {
                    bufferOffset = 0;
                    bufferLimit = cipher.doFinal(plaintextBuffer, 0);

                    // Validate MAC
                    var expectedCiphertextMac = new byte[MAC_LENGTH];
                    var expectedCiphertextMacOffset = 0;
                    while (expectedCiphertextMacOffset < expectedCiphertextSha256.length) {
                        var read = rawInputStream.read(expectedCiphertextMac, expectedCiphertextMacOffset, MAC_LENGTH - expectedCiphertextMacOffset);
                        if (read == -1) {
                            throw new IOException("Unexpected end of stream");
                        }

                        expectedCiphertextMacOffset += read;
                    }

                    if (ciphertextDigest != null) {
                        ciphertextDigest.update(expectedCiphertextMac);
                        var actualCipherTextSha256 = ciphertextDigest.digest();
                        if (!Arrays.equals(expectedCiphertextSha256, actualCipherTextSha256)) {
                            throw new MediaDownloadException("Ciphertext SHA256 hash doesn't match the expected value");
                        }
                    }

                    var actualCiphertextMac = mac.doFinal();
                    if (!Arrays.equals(expectedCiphertextMac, 0, MAC_LENGTH, actualCiphertextMac, 0, MAC_LENGTH)) {
                        throw new HmacValidationException("media_decryption");
                    }

                    finished = true;
                } else {
                    var toRead = remainingCiphertext < ciphertextBuffer.length ? Math.toIntExact(remainingCiphertext) : ciphertextBuffer.length;
                    var read = rawInputStream.read(ciphertextBuffer, 0, toRead);
                    if (read == 0) {
                        throw new IOException("Unexpected end of stream: expected " + remainingCiphertext + " more bytes");
                    }

                    remainingCiphertext -= read;

                    if (ciphertextDigest != null) {
                        ciphertextDigest.update(ciphertextBuffer, 0, read);
                    }
                    mac.update(ciphertextBuffer, 0, read);

                    bufferOffset = 0;
                    bufferLimit = cipher.update(ciphertextBuffer, 0, read, plaintextBuffer, 0);
                }

                if (plaintextDigest != null && bufferLimit > 0) {
                    plaintextDigest.update(plaintextBuffer, bufferOffset, bufferLimit - bufferOffset);
                }
            }

            if (finished && bufferOffset >= bufferLimit && !validated) {
                if (plaintextDigest != null) {
                    var actualPlaintextSha256 = plaintextDigest.digest();
                    if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                        throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
                    }
                }
                validated = true;
            }

            return finished && bufferOffset >= bufferLimit;
        } catch (GeneralSecurityException exception) {
            throw new IOException("Cannot decrypt data", exception);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        rawInputStream.close();
    }
}
