package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static it.auties.whatsapp.model.message.model.MediaMessageType.STICKER;
import static java.util.Objects.requireNonNullElse;

/**
 * A model class that represents a message holding a sticker inside
 */
@ProtobufMessage(name = "Message.StickerMessage")
public final class StickerMessage extends MediaMessage<StickerMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private String mediaUrl;
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    private byte[] mediaSha256;
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private byte[] mediaEncryptedSha256;
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private byte[] mediaKey;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final String mimetype;
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    private final Integer height;
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    private final Integer width;
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    private String mediaDirectPath;
    @ProtobufProperty(index = 9, type = ProtobufType.UINT64)
    private Long mediaSize;
    @ProtobufProperty(index = 10, type = ProtobufType.UINT64)
    private Long mediaKeyTimestampSeconds;
    @ProtobufProperty(index = 11, type = ProtobufType.UINT32)
    private final Integer firstFrameLength;
    @ProtobufProperty(index = 12, type = ProtobufType.BYTES)
    private final byte[] firstFrameSidecar;
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    private final boolean animated;
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 18, type = ProtobufType.INT64)
    private final Long stickerSentTimestamp;
    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    private final boolean avatar;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StickerMessage(String mediaUrl, byte[] mediaSha256, byte[] mediaEncryptedSha256, byte[] mediaKey, String mimetype, Integer height, Integer width, String mediaDirectPath, Long mediaSize, Long mediaKeyTimestampSeconds, Integer firstFrameLength, byte[] firstFrameSidecar, boolean animated, byte[] thumbnail, ContextInfo contextInfo, Long stickerSentTimestamp, boolean avatar) {
        this.mediaUrl = mediaUrl;
        this.mediaSha256 = mediaSha256;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaKey = mediaKey;
        this.mimetype = mimetype;
        this.height = height;
        this.width = width;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaSize = mediaSize;
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        this.firstFrameLength = firstFrameLength;
        this.firstFrameSidecar = firstFrameSidecar;
        this.animated = animated;
        this.thumbnail = thumbnail;
        this.contextInfo = contextInfo;
        this.stickerSentTimestamp = stickerSentTimestamp;
        this.avatar = avatar;
    }

    @ProtobufBuilder(className = "SimpleStickerMessageBuilder")
    static StickerMessage simpleBuilder(byte[] media, String mimeType, byte[] thumbnail, boolean animated, ContextInfo contextInfo) {
        return new StickerMessageBuilder()
                .mimetype(requireNonNullElse(mimeType, STICKER.mimeType()))
                .thumbnail(thumbnail != null ? thumbnail : Medias.getImageThumbnail(media, false).orElse(null))
                .animated(animated)
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::empty))
                .build()
                .setDecodedMedia(media);
    }

    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    public boolean animated() {
        return animated;
    }

    public boolean avatar() {
        return avatar;
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    @Override
    public StickerMessage setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public StickerMessage setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public StickerMessage setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public StickerMessage setMediaKeyTimestamp(Long timestamp) {
        this.mediaKeyTimestampSeconds = timestamp;
        return this;
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public StickerMessage setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public StickerMessage setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
        return this;
    }

    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    @Override
    public OptionalLong mediaKeyTimestampSeconds() {
        return Clock.parseTimestamp(mediaKeyTimestampSeconds);
    }

    @Override
    public Optional<ZonedDateTime> mediaKeyTimestamp() {
        return Clock.parseSeconds(mediaKeyTimestampSeconds);
    }

    @Override
    public StickerMessage setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
        return this;
    }

    @Override
    public MediaMessageType mediaType() {
        return MediaMessageType.STICKER;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    public OptionalInt firstFrameLength() {
        return firstFrameLength == null ? OptionalInt.empty() : OptionalInt.of(firstFrameLength);
    }

    public Optional<byte[]> firstFrameSidecar() {
        return Optional.ofNullable(firstFrameSidecar);
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public OptionalLong stickerSentTimestamp() {
        return Clock.parseTimestamp(stickerSentTimestamp);
    }

    @Override
    public StickerMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }
}