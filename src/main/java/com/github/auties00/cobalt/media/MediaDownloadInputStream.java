package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.exception.HmacValidationException;
import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.model.media.MediaProvider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public abstract class MediaDownloadInputStream extends MediaInputStream {
    final Inflater inflater;
    MediaDownloadInputStream(InputStream rawInputStream, boolean inflate) {
        Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
        var inflater = inflate ? new Inflater() : null;
        super(rawInputStream);
        this.inflater = inflater;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if(inflater != null) {
            inflater.end();
        }
    }

    static MediaDownloadInputStream ofCiphertext(InputStream rawInputStream, int payloadLength, MediaProvider provider) throws GeneralSecurityException {
        return new Ciphertext(rawInputStream, payloadLength, provider);
    }

    static MediaDownloadInputStream ofPlaintext(InputStream rawInputStream, int payloadLength, MediaProvider provider) throws NoSuchAlgorithmException {
        return new Plaintext(rawInputStream, payloadLength, provider);
    }

    private static final class Ciphertext extends MediaDownloadInputStream {
        private final byte[] ciphertextBuffer;
        private final byte[] plaintextBuffer;
        private final byte[] outputBuffer;
        private final MessageDigest plaintextDigest;
        private final MessageDigest ciphertextDigest;
        private final Mac mac;
        private final Cipher cipher;
        private final byte[] expectedPlaintextSha256;
        private final byte[] expectedCiphertextSha256;

        private int plaintextOffset = 0;
        private int plaintextLimit = 0;
        private int outputOffset = 0;
        private int outputLimit = 0;
        private long remainingCiphertext;
        private boolean finished = false;
        private boolean validated = false;

        public Ciphertext(InputStream rawInputStream, int payloadLength, MediaProvider provider) throws GeneralSecurityException {
            Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
            Objects.requireNonNull(provider, "provider must not be null");

            super(rawInputStream, provider.mediaPath().inflatable());
            this.ciphertextBuffer = new byte[BUFFER_LENGTH];
            this.plaintextBuffer = new byte[BUFFER_LENGTH];
            this.outputBuffer = inflater != null ? new byte[BUFFER_LENGTH] : plaintextBuffer;
            this.expectedPlaintextSha256 = provider.mediaSha256().orElse(null);
            this.plaintextDigest = expectedPlaintextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;

            this.expectedCiphertextSha256 = provider.mediaEncryptedSha256().orElse(null);
            this.ciphertextDigest = expectedCiphertextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;

            var mediaKey = provider.mediaKey()
                    .orElseThrow(() -> new MediaDownloadException("Media key must be present"));
            var keyName = provider.mediaPath()
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
            if (ensureOutputDataAvailable()) {
                return -1;
            }
            return outputBuffer[outputOffset++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (ensureOutputDataAvailable()) {
                return -1;
            }

            var available = outputLimit - outputOffset;
            var toRead = Math.min(len, available);
            System.arraycopy(outputBuffer, outputOffset, b, off, toRead);
            outputOffset += toRead;
            return toRead;
        }

        private boolean ensureOutputDataAvailable() throws IOException {
            if (inflater != null) {
                return ensureInflatedDataAvailable();
            } else {
                outputOffset = plaintextOffset;
                outputLimit = plaintextLimit;
                return ensurePlaintextDataAvailable();
            }
        }

        private boolean ensureInflatedDataAvailable() throws IOException {
            try {
                while (outputOffset >= outputLimit && !inflater.finished()) {
                    if (inflater.needsInput()) {
                        if (ensurePlaintextDataAvailable()) {
                            break;
                        }
                        var available = plaintextLimit - plaintextOffset;
                        inflater.setInput(plaintextBuffer, plaintextOffset, available);
                        plaintextOffset += available;
                    }

                    outputOffset = 0;
                    outputLimit = inflater.inflate(outputBuffer);
                }

                return inflater.finished() && outputOffset >= outputLimit;
            } catch (DataFormatException exception) {
                throw new IOException("Cannot inflate data", exception);
            }
        }

        private boolean ensurePlaintextDataAvailable() throws IOException {
            try {
                while (plaintextOffset >= plaintextLimit && !finished) {
                    if (remainingCiphertext == 0) {
                        plaintextOffset = 0;
                        plaintextLimit = cipher.doFinal(plaintextBuffer, 0);

                        // Validate MAC
                        var expectedCiphertextMac = new byte[MAC_LENGTH];
                        var expectedCiphertextMacOffset = 0;
                        while (expectedCiphertextMacOffset < MAC_LENGTH) {
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
                        if (read == -1) {
                            throw new IOException("Unexpected end of stream: expected " + remainingCiphertext + " more bytes");
                        }

                        remainingCiphertext -= read;

                        if (ciphertextDigest != null) {
                            ciphertextDigest.update(ciphertextBuffer, 0, read);
                        }
                        mac.update(ciphertextBuffer, 0, read);

                        plaintextOffset = 0;
                        plaintextLimit = cipher.update(ciphertextBuffer, 0, read, plaintextBuffer, 0);
                    }

                    if (plaintextDigest != null && plaintextLimit > 0) {
                        plaintextDigest.update(plaintextBuffer, plaintextOffset, plaintextLimit - plaintextOffset);
                    }
                }

                if (finished && plaintextOffset >= plaintextLimit && !validated) {
                    if (plaintextDigest != null) {
                        var actualPlaintextSha256 = plaintextDigest.digest();
                        if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                            throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
                        }
                    }
                    validated = true;
                }

                return finished && plaintextOffset >= plaintextLimit;
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

    private static final class Plaintext extends MediaDownloadInputStream {
        private final byte[] plaintextBuffer;
        private final byte[] outputBuffer;
        private final MessageDigest plaintextDigest;
        private final byte[] expectedPlaintextSha256;
        private int plaintextOffset = 0;
        private int plaintextLimit = 0;
        private int outputOffset = 0;
        private int outputLimit = 0;
        private long remainingBytes;
        private boolean validated = false;

        public Plaintext(InputStream rawInputStream, int payloadLength, MediaProvider provider) throws NoSuchAlgorithmException {
            Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
            Objects.requireNonNull(provider, "provider must not be null");

            super(rawInputStream, provider.mediaPath().inflatable());
            this.plaintextBuffer = inflater != null ? new byte[BUFFER_LENGTH] : null;
            this.outputBuffer = inflater != null ? new byte[BUFFER_LENGTH] : null;
            this.expectedPlaintextSha256 = provider.mediaSha256().orElse(null);
            this.plaintextDigest = expectedPlaintextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;
            this.remainingBytes = payloadLength;
        }

        @Override
        public int read() throws IOException {
            if (inflater != null) {
                if (ensureOutputDataAvailable()) {
                    return -1;
                }
                return outputBuffer[outputOffset++] & 0xFF;
            } else {
                if (remainingBytes <= 0) {
                    validateIfNeeded();
                    return -1;
                }

                var b = rawInputStream.read();
                if (b == -1) {
                    throw new IOException("Unexpected end of stream");
                }

                remainingBytes--;
                if (plaintextDigest != null) {
                    plaintextDigest.update((byte) b);
                }

                if (remainingBytes == 0) {
                    validateIfNeeded();
                }

                return b;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (inflater != null) {
                if (ensureOutputDataAvailable()) {
                    return -1;
                }

                var available = outputLimit - outputOffset;
                var toRead = Math.min(len, available);
                System.arraycopy(outputBuffer, outputOffset, b, off, toRead);
                outputOffset += toRead;
                return toRead;
            } else {
                if (remainingBytes <= 0) {
                    validateIfNeeded();
                    return -1;
                }

                var toRead = (int) Math.min(len, remainingBytes);
                var read = rawInputStream.read(b, off, toRead);
                if (read == -1) {
                    throw new IOException("Unexpected end of stream");
                }

                remainingBytes -= read;
                if (plaintextDigest != null) {
                    plaintextDigest.update(b, off, read);
                }

                if (remainingBytes == 0) {
                    validateIfNeeded();
                }

                return read;
            }
        }

        private boolean ensureOutputDataAvailable() throws IOException {
            try {
                while (outputOffset >= outputLimit && !inflater.finished()) {
                    if (inflater.needsInput()) {
                        if (ensurePlaintextDataAvailable()) {
                            break;
                        }
                        var available = plaintextLimit - plaintextOffset;
                        inflater.setInput(plaintextBuffer, plaintextOffset, available);
                        plaintextOffset += available;
                    }

                    outputOffset = 0;
                    outputLimit = inflater.inflate(outputBuffer);
                }

                return inflater.finished() && outputOffset >= outputLimit;
            } catch (DataFormatException exception) {
                throw new IOException("Cannot inflate data", exception);
            }
        }

        private boolean ensurePlaintextDataAvailable() throws IOException {
            while (plaintextOffset >= plaintextLimit && remainingBytes > 0) {
                var toRead = (int) Math.min(plaintextBuffer.length, remainingBytes);
                var read = rawInputStream.read(plaintextBuffer, 0, toRead);
                if (read == -1) {
                    throw new IOException("Unexpected end of stream");
                }

                remainingBytes -= read;
                if (plaintextDigest != null) {
                    plaintextDigest.update(plaintextBuffer, 0, read);
                }

                plaintextOffset = 0;
                plaintextLimit = read;

                if (remainingBytes == 0) {
                    validateIfNeeded();
                }
            }

            return remainingBytes <= 0 && plaintextOffset >= plaintextLimit;
        }

        private void validateIfNeeded() {
            if (!validated && plaintextDigest != null) {
                var actualPlaintextSha256 = plaintextDigest.digest();
                if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                    throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
                }
                validated = true;
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            rawInputStream.close();
        }
    }
}