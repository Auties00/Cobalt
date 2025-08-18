package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalLong;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "Message.HistorySyncNotification")
public final class HistorySyncNotification implements MutableAttachmentProvider {
    @ProtobufProperty(index = 1, type = BYTES)
    byte[] mediaSha256;
    @ProtobufProperty(index = 2, type = UINT64)
    Long mediaSize;
    @ProtobufProperty(index = 3, type = BYTES)
    byte[] mediaKey;
    @ProtobufProperty(index = 4, type = BYTES)
    byte[] mediaEncryptedSha256;
    @ProtobufProperty(index = 5, type = STRING)
    String mediaDirectPath;
    @ProtobufProperty(index = 6, type = ENUM)
    final HistorySync.Type syncType;
    @ProtobufProperty(index = 7, type = UINT32)
    final Integer chunkOrder;
    @ProtobufProperty(index = 8, type = STRING)
    final String originalMessageId;
    @ProtobufProperty(index = 9, type = UINT32)
    final Integer progress;
    @ProtobufProperty(index = 10, type = INT64)
    final long oldestMsgInChunkTimestampSec;
    @ProtobufProperty(index = 11, type = BYTES)
    final ByteBuffer initialHistBootstrapInlinePayload;
    @ProtobufProperty(index = 12, type = STRING)
    final String peerDataRequestSessionId;

    public HistorySyncNotification(byte[] mediaSha256, Long mediaSize, byte[] mediaKey, byte[] mediaEncryptedSha256, String mediaDirectPath, HistorySync.Type syncType, Integer chunkOrder, String originalMessageId, Integer progress, long oldestMsgInChunkTimestampSec, ByteBuffer initialHistBootstrapInlinePayload, String peerDataRequestSessionId) {
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.mediaKey = mediaKey;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.syncType = syncType;
        this.chunkOrder = chunkOrder;
        this.originalMessageId = originalMessageId;
        this.progress = progress;
        this.oldestMsgInChunkTimestampSec = oldestMsgInChunkTimestampSec;
        this.initialHistBootstrapInlinePayload = initialHistBootstrapInlinePayload;
        this.peerDataRequestSessionId = peerDataRequestSessionId;
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
        return AttachmentType.HISTORY_SYNC;
    }

    public HistorySync.Type syncType() {
        return syncType;
    }

    public Integer chunkOrder() {
        return chunkOrder;
    }

    public Optional<String> originalMessageId() {
        return Optional.ofNullable(originalMessageId);
    }


    public Integer progress() {
        return progress;
    }

    public long oldestMsgInChunkTimestampSec() {
        return oldestMsgInChunkTimestampSec;
    }

    public Optional<ByteBuffer> initialHistBootstrapInlinePayload() {
        return Optional.ofNullable(initialHistBootstrapInlinePayload);
    }

    public Optional<String> peerDataRequestSessionId() {
        return Optional.ofNullable(peerDataRequestSessionId);
    }

    @ProtobufEnum(name = "Message.HistorySyncNotification.HistorySyncType")
    public enum Type {
        INITIAL_BOOTSTRAP(0),
        INITIAL_STATUS_V3(1),
        FULL(2),
        RECENT(3),
        PUSH_NAME(4),
        NON_BLOCKING_DATA(5),
        ON_DEMAND(6);

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}
