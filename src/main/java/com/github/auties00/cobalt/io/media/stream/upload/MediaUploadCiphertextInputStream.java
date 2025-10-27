package com.github.auties00.cobalt.io.media.stream.upload;

import com.github.auties00.cobalt.util.SecureBytes;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Optional;

public final class MediaUploadCiphertextInputStream extends MediaUploadInputStream {
    private final MessageDigest plaintextDigest;
    private final MessageDigest ciphertextDigest;
    private final Mac ciphertextMac;
    private final Cipher cipher;
    private final byte[] plaintextBuffer;
    private final byte[] ciphertextBuffer;
    private final byte[] outputBuffer;

    private final byte[] mediaKey;

    private byte[] plaintextHash;
    private byte[] ciphertextHash;

    private long plaintextLength;
    private boolean finalized;
    private int outputPosition;
    private int outputLimit;

    public MediaUploadCiphertextInputStream(InputStream rawInputStream, String keyName) {
        super(rawInputStream);

        try {
            this.plaintextDigest = MessageDigest.getInstance("SHA-256");
            this.ciphertextDigest = MessageDigest.getInstance("SHA-256");

            this.mediaKey = SecureBytes.random(32);
            var expanded = deriveMediaKeyData(mediaKey, keyName);
            var iv = new IvParameterSpec(expanded, 0, IV_LENGTH);
            var cipherKey = new SecretKeySpec(expanded, IV_LENGTH, KEY_LENGTH, "AES");
            var macKey = new SecretKeySpec(expanded, IV_LENGTH + KEY_LENGTH, KEY_LENGTH, "HmacSHA256");

            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey, iv);

            this.ciphertextMac = Mac.getInstance("HmacSHA256");
            ciphertextMac.init(macKey);
            ciphertextMac.update(expanded, 0, IV_LENGTH);

            this.plaintextBuffer = new byte[BUFFER_LENGTH];
            this.ciphertextBuffer = new byte[BUFFER_LENGTH + cipher.getBlockSize()];
            this.outputBuffer = new byte[BUFFER_LENGTH];
            this.plaintextLength = 0;
        }catch (GeneralSecurityException exception) {
            throw new InternalError("Cannot initialize stream", exception);
        }
    }

    @Override
    public int read() throws IOException {
        ensureDataAvailable();
        if (outputPosition >= outputLimit) {
            return -1;
        }

        return outputBuffer[outputPosition++] & 0xFF;
    }

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
                    ciphertextDigest.update(mac, 0, MAC_LENGTH);

                    var macSpace = outputBuffer.length - outputLimit;
                    var macToCopy = Math.min(MAC_LENGTH, macSpace);
                    System.arraycopy(mac, 0, outputBuffer, outputLimit, macToCopy);
                    outputLimit += macToCopy;

                    plaintextHash = plaintextDigest.digest();
                    ciphertextHash = ciphertextDigest.digest();

                    finalized = true;
                    break;
                }

                plaintextDigest.update(plaintextBuffer, 0, plaintextRead);
                plaintextLength += plaintextRead;

                var ciphertextLen = cipher.update(plaintextBuffer, 0, plaintextRead, ciphertextBuffer, 0);
                processChunk(ciphertextLen);
            }
        } catch (GeneralSecurityException exception) {
            throw new IOException("Cannot encrypt data", exception);
        }
    }

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

    @Override
    public void close() throws IOException {
        rawInputStream.close();
    }

    @Override
    public long fileLength() {
        return plaintextLength;
    }

    @Override
    public byte[] fileSha256() {
        if(plaintextHash == null) {
            throw new IllegalStateException("Cannot get file SHA-256 hash before the file has been fully read");
        }

        return plaintextHash;
    }

    @Override
    public Optional<byte[]> fileEncSha256() {
        if(ciphertextHash == null) {
            throw new IllegalStateException("Cannot get file encrypted SHA-256 hash before the file has been fully read");
        }

        return Optional.of(ciphertextHash);
    }

    @Override
    public Optional<byte[]> fileKey() {
        return Optional.of(mediaKey);
    }
}
