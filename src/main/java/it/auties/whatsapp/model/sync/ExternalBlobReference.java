package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;

import java.util.Optional;
import java.util.OptionalLong;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "ExternalBlobReference")
public final class ExternalBlobReference implements MutableAttachmentProvider {
    @ProtobufProperty(index = 1, type = BYTES)
    byte[] mediaKey;
    @ProtobufProperty(index = 2, type = STRING)
    String mediaDirectPath;
    @ProtobufProperty(index = 3, type = STRING)
    final String handle;
    @ProtobufProperty(index = 4, type = UINT64)
    long mediaSize;
    @ProtobufProperty(index = 5, type = BYTES)
    byte[] mediaSha256;
    @ProtobufProperty(index = 6, type = BYTES)
    byte[] mediaEncryptedSha256;

    public ExternalBlobReference(byte[] mediaKey,
                                 String mediaDirectPath,
                                 String handle,
                                 long mediaSize,
                                 byte[] mediaSha256,
                                 byte[] mediaEncryptedSha256) {
        this.mediaKey = mediaKey;
        this.mediaDirectPath = mediaDirectPath;
        this.handle = handle;
        this.mediaSize = mediaSize;
        this.mediaSha256 = mediaSha256;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.empty();
    }

    @Override
    public void setMediaUrl(String mediaUrl) {
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public void setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
    }

    @Override
    public void setMediaKeyTimestamp(Long timestamp) {
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public void setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public void setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
    }

    @Override
    public OptionalLong mediaSize() {
        return mediaSize == 0 ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.APP_STATE;
    }

    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }
}
