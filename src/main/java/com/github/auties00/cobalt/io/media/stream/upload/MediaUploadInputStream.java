package com.github.auties00.cobalt.io.media.stream.upload;

import com.github.auties00.cobalt.io.media.stream.MediaInputStream;

import java.io.InputStream;
import java.util.Optional;

public abstract sealed class MediaUploadInputStream
        extends MediaInputStream
        permits MediaUploadCiphertextInputStream, MediaUploadPlaintextInputStream {
    protected static final int BUFFER_LENGTH = 8192;
    protected static final int MAC_LENGTH = 10;

    protected MediaUploadInputStream(InputStream rawInputStream) {
        super(rawInputStream);
    }

    public abstract long fileLength();

    public abstract byte[] fileSha256();

    public abstract Optional<byte[]> fileEncSha256();

    public abstract Optional<byte[]> fileKey();
}
