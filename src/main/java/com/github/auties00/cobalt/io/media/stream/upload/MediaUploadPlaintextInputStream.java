package com.github.auties00.cobalt.io.media.stream.upload;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Optional;

public final class MediaUploadPlaintextInputStream extends MediaUploadInputStream {
    private final MessageDigest plaintextDigest;
    private long plaintextLength;

    private byte[] plaintextHash;
    private boolean finalized;

    public MediaUploadPlaintextInputStream(InputStream rawInputStream) {
        super(rawInputStream);
        try {
            this.plaintextDigest = MessageDigest.getInstance("SHA-256");
            this.plaintextLength = 0;
        }catch (GeneralSecurityException exception) {
            throw new InternalError("Cannot initialize stream", exception);
        }
    }

    @Override
    public int read() throws IOException {
        var ch = rawInputStream.read();
        if (ch != -1) {
            plaintextDigest.update((byte) ch);
            plaintextLength++;
        }else if(!finalized) {
            finalized = true;
            plaintextHash = plaintextDigest.digest();
        }
        return ch;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        var result = rawInputStream.read(b, off, len);
        if (result != -1) {
            plaintextDigest.update(b, off, result);
            plaintextLength += result;
        }else if(!finalized) {
            finalized = true;
            plaintextHash = plaintextDigest.digest();
        }
        return result;
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
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> fileKey() {
        return Optional.empty();
    }
}
