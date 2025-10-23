package com.github.auties00.cobalt.io.media.download;

import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.model.media.MediaProvider;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

public final class MediaDownloadPlaintextInputStream extends MediaDownloadInputStream {
    private final MessageDigest plaintextDigest;
    private final byte[] expectedPlaintextSha256;
    private long remainingBytes;
    private boolean validated = false;

    public MediaDownloadPlaintextInputStream(InputStream rawInputStream, int payloadLength, MediaProvider provider) throws NoSuchAlgorithmException {
        Objects.requireNonNull(rawInputStream, "rawInputStream must not be null");
        Objects.requireNonNull(provider, "provider must not be null");

        super(rawInputStream, provider.mediaPath().inflatable());
        this.expectedPlaintextSha256 = provider.mediaSha256().orElse(null);
        this.plaintextDigest = expectedPlaintextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;
        this.remainingBytes = payloadLength;
    }

    @Override
    public int read() throws IOException {
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

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
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
