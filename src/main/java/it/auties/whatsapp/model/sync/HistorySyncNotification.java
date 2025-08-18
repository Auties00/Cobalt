package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;

import java.util.Optional;
import java.util.OptionalLong;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "Message.HistorySyncNotification")
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
    @ProtobufProperty(index = 6, type = ENUM)
    private final HistorySync.Type syncType;
    @ProtobufProperty(index = 7, type = UINT32)
    private final Integer chunkOrder;
    @ProtobufProperty(index = 8, type = STRING)
    private final String originalMessageId;
    @ProtobufProperty(index = 9, type = UINT32)
    private final Integer progress;
    @ProtobufProperty(index = 10, type = INT64)
    private final long oldestMsgInChunkTimestampSec;
    @ProtobufProperty(index = 11, type = BYTES)
    private final byte[] initialHistBootstrapInlinePayload;
    @ProtobufProperty(index = 12, type = STRING)
    private final String peerDataRequestSessionId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HistorySyncNotification(byte[] mediaSha256, Long mediaSize, byte[] mediaKey, byte[] mediaEncryptedSha256, String mediaDirectPath, HistorySync.Type syncType, Integer chunkOrder, String originalMessageId, Integer progress, long oldestMsgInChunkTimestampSec, byte[] initialHistBootstrapInlinePayload, String peerDataRequestSessionId) {
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
    public HistorySyncNotification setMediaKeyTimestamp(Long timestamp) {
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

    public Optional<byte[]> initialHistBootstrapInlinePayload() {
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
