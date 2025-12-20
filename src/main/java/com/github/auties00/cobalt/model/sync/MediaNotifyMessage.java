package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "MediaNotifyMessage")
public final class MediaNotifyMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String expressPathUrl;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] fileEncSha256;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final long fileLength;

    public MediaNotifyMessage(String expressPathUrl, byte[] fileEncSha256, long fileLength) {
        this.expressPathUrl = expressPathUrl;
        this.fileEncSha256 = fileEncSha256;
        this.fileLength = fileLength;
    }

    public Optional<String> expressPathUrl() {
        return Optional.ofNullable(expressPathUrl);
    }

    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(fileEncSha256);
    }

    public long fileLength() {
        return fileLength;
    }
}
