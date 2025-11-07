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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.Inflater;

abstract class MediaDownloadInputStream extends MediaInputStream {
    final HttpClient client;
    final Inflater inflater;
    MediaDownloadInputStream(HttpClient client, InputStream rawInputStream, boolean inflate) {
        Objects.requireNonNull(client, "client cannot be null");
        Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
        super(rawInputStream);
        this.client = client;
        this.inflater = inflate ? new Inflater() : null;
    }

    static Optional<MediaDownloadInputStream> of(MediaProvider provider, String uploadUrl) {
        var hasKeyName = provider.mediaPath()
                .keyName()
                .isPresent();
        var hasMediaKey = provider.mediaKey()
                .isPresent();
        if (hasKeyName != hasMediaKey) {
            throw new MediaDownloadException("Media key and key name must both be present or both be absent");
        }

        var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if(response.statusCode() != 200) {
                throw new MediaDownloadException("Cannot download media: status code " + response.statusCode());
            }

            var payloadLength = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElseThrow(() -> new MediaDownloadException("Unknown content length"));

            var rawInputStream = response.body();
            if(hasKeyName) {
                return Optional.of(new Ciphertext(client, rawInputStream, payloadLength, provider));
            }else {
                return Optional.of(new Plaintext(client, rawInputStream, payloadLength, provider));
            }
        }catch (Throwable throwable) {
            client.close();
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        client.close();
        if(inflater != null) {
            inflater.end();
        }
    }

    private static final class Ciphertext extends MediaDownloadInputStream {
        private final byte[] buffer;
        private int bufferOffset;
        private int bufferLimit;

        private final MessageDigest plaintextDigest;
        private final MessageDigest ciphertextDigest;

        private final Mac mac;

        private final Cipher cipher;

        private final byte[] expectedPlaintextSha256;
        private final byte[] expectedCiphertextSha256;

        private long remainingCiphertext;

        public Ciphertext(HttpClient client, InputStream rawInputStream, long payloadLength, MediaProvider provider) throws GeneralSecurityException {
            Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
            Objects.requireNonNull(provider, "provider must not be null");

            super(client, rawInputStream, provider.mediaPath().inflatable());
            this.buffer = new byte[BUFFER_LENGTH];
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
            if (isFinished()) {
                return -1;
            }
            return buffer[bufferOffset++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (isFinished()) {
                return -1;
            }

            var available = bufferLimit - bufferOffset;
            var toRead = Math.min(len, available);
            System.arraycopy(buffer, bufferOffset, b, off, toRead);
            bufferOffset += toRead;
            return toRead;
        }

        private boolean isFinished() throws IOException {
            if(finished) {
                return true;
            }

            readData();
            return bufferOffset >= bufferLimit;
        }

        private void readData() throws IOException {
            try {
                while (bufferOffset >= bufferLimit && !finished) {
                    if (remainingCiphertext == 0) {
                        finished = true;

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

                        bufferOffset = 0;
                        bufferLimit = cipher.doFinal(buffer, bufferOffset);

                        if(plaintextDigest != null) {
                            plaintextDigest.update(buffer, bufferOffset, bufferLimit - bufferOffset);
                            var actualPlaintextSha256 = plaintextDigest.digest();
                            if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                                bufferLimit = 0;
                                throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
                            }
                        }
                    } else {
                        var toRead = (int) Math.min(buffer.length, remainingCiphertext);
                        System.out.println("Reading");
                        var read = rawInputStream.read(buffer, 0, toRead);
                        System.out.println("Read " + read);
                        if (read == -1) {
                            throw new IOException("Unexpected end of stream: expected " + remainingCiphertext + " more bytes");
                        }
                        remainingCiphertext -= read;

                        if (ciphertextDigest != null) {
                            ciphertextDigest.update(buffer, 0, read);
                        }

                        mac.update(buffer, 0, read);

                        bufferOffset = 0;
                        bufferLimit = cipher.update(buffer, 0, read, buffer, bufferOffset);
                        System.out.println("Decrypted " + bufferLimit + " bytes");

                        if(plaintextDigest != null) {
                            plaintextDigest.update(buffer, bufferOffset, bufferLimit - bufferOffset);
                        }
                    }
                }
            }catch (GeneralSecurityException exception) {
                throw new MediaDownloadException("Cannot read data", exception);
            }
        }
    }

    private static final class Plaintext extends MediaDownloadInputStream {
        private final MessageDigest plaintextDigest;
        private final byte[] expectedPlaintextSha256;

        private long remainingPlaintext;

        private boolean validated;

        public Plaintext(HttpClient client, InputStream rawInputStream, long payloadLength, MediaProvider provider) throws NoSuchAlgorithmException {
            Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
            Objects.requireNonNull(provider, "provider must not be null");

            super(client, rawInputStream, provider.mediaPath().inflatable());
            this.expectedPlaintextSha256 = provider.mediaSha256().orElse(null);
            this.plaintextDigest = expectedPlaintextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;
            this.remainingPlaintext = payloadLength;
        }

        @Override
        public int read() throws IOException {
            if (remainingPlaintext == 0) {
                return -1;
            }

            var b = rawInputStream.read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream");
            }

            if (plaintextDigest != null) {
                plaintextDigest.update((byte) b);
            }

            if (--remainingPlaintext == 0) {
                validateIfNeeded();
            }

            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remainingPlaintext == 0) {
                return -1;
            }

            var toRead = (int) Math.min(len, remainingPlaintext);
            var read = rawInputStream.read(b, off, toRead);
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }

            if (plaintextDigest != null) {
                plaintextDigest.update(b, off, read);
            }

            remainingPlaintext -= read;
            if (remainingPlaintext == 0) {
                validateIfNeeded();
            }

            return read;
        }

        private void validateIfNeeded() {
            if (validated || plaintextDigest == null) {
                return;
            }

            validated = true;

            var actualPlaintextSha256 = plaintextDigest.digest();
            if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
            }
        }
    }
}