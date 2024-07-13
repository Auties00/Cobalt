package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;

import java.util.Optional;
import java.util.OptionalLong;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "ExternalBlobReference")
public final class ExternalBlobReference implements MutableAttachmentProvider<ExternalBlobReference> {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] mediaKey;
    @ProtobufProperty(index = 2, type = STRING)
    private String mediaDirectPath;
    @ProtobufProperty(index = 3, type = STRING)
    private final String handle;
    @ProtobufProperty(index = 4, type = UINT64)
    private long mediaSize;
    @ProtobufProperty(index = 5, type = BYTES)
    private byte[] mediaSha256;
    @ProtobufProperty(index = 6, type = BYTES)
    private byte[] mediaEncryptedSha256;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
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
    public ExternalBlobReference setMediaUrl(String mediaUrl) {
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public ExternalBlobReference setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public ExternalBlobReference setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public ExternalBlobReference setMediaKeyTimestamp(Long timestamp) {
        return this;
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public ExternalBlobReference setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public ExternalBlobReference setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
        return this;
    }

    @Override
    public OptionalLong mediaSize() {
        return mediaSize == 0 ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    @Override
    public ExternalBlobReference setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
        return this;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.APP_STATE;
    }

    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }
}
