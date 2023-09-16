package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;

import java.util.Optional;
import java.util.OptionalLong;

import static it.auties.protobuf.model.ProtobufType.*;

public final class HistorySyncNotification implements MutableAttachmentProvider<HistorySyncNotification> {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] mediaSha256;
    @ProtobufProperty(index = 2, type = UINT64)
    private Long mediaSize;
    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] mediaKey;
    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] mediaEncryptedSha256;
    @ProtobufProperty(index = 5, type = STRING)
    private String mediaDirectPath;
    @ProtobufProperty(index = 6, type = OBJECT)
    private final HistorySyncType syncType;
    @ProtobufProperty(index = 7, type = UINT32)
    private final Integer chunkOrder;
    @ProtobufProperty(index = 8, type = STRING)
    private final String originalMessageId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HistorySyncNotification(byte[] mediaSha256, long mediaSize, byte[] mediaKey, byte[] mediaEncryptedSha256, String mediaDirectPath, HistorySyncType syncType, Integer chunkOrder, String originalMessageId) {
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.mediaKey = mediaKey;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.syncType = syncType;
        this.chunkOrder = chunkOrder;
        this.originalMessageId = originalMessageId;
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.empty();
    }

    @Override
    public HistorySyncNotification setMediaUrl(String mediaUrl) {
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public HistorySyncNotification setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public HistorySyncNotification setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public HistorySyncNotification setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public HistorySyncNotification setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
        return this;
    }

    @Override
    public OptionalLong mediaSize() {
        return mediaSize == 0 ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    @Override
    public HistorySyncNotification setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
        return this;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.HISTORY_SYNC;
    }

    public HistorySyncType syncType() {
        return syncType;
    }

    public int chunkOrder() {
        return chunkOrder;
    }

    public Optional<String> originalMessageId() {
        return Optional.ofNullable(originalMessageId);
    }
}
