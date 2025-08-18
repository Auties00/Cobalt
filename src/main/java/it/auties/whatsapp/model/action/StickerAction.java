package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.media.MutableAttachmentProvider;

import java.util.*;


/**
 * A model clas that represents a sticker
 */
@ProtobufMessage(name = "SyncActionValue.StickerAction")
public final class StickerAction implements Action, MutableAttachmentProvider {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] fileEncSha256;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaKey;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String mimetype;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    final Integer height;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    final Integer width;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String mediaDirectPath;

    @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
    Long mediaSize;

    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    final boolean favorite;

    @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
    final Integer deviceIdHint;

    StickerAction(String mediaUrl, byte[] fileEncSha256, byte[] mediaKey, String mimetype, Integer height, Integer width, String mediaDirectPath, Long mediaSize, boolean favorite, Integer deviceIdHint) {
        this.mediaUrl = mediaUrl;
        this.fileEncSha256 = fileEncSha256;
        this.mediaKey = mediaKey;
        this.mimetype = mimetype;
        this.height = height;
        this.width = width;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaSize = mediaSize;
        this.favorite = favorite;
        this.deviceIdHint = deviceIdHint;
    }


    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    @Override
    public int actionVersion() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    @Override
    public void setMediaKeyTimestamp(Long timestamp) {
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.empty();
    }

    @Override
    public void setMediaSha256(byte[] bytes) {
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(fileEncSha256);
    }

    @Override
    public void setMediaEncryptedSha256(byte[] fileEncSha256) {
        this.fileEncSha256 = fileEncSha256;
    }

    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    @Override
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
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
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.STICKER;
    }

    public boolean favorite() {
        return favorite;
    }

    public OptionalInt deviceIdHint() {
        return deviceIdHint == null ? OptionalInt.empty() : OptionalInt.of(deviceIdHint);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StickerAction that
                && Objects.equals(height, that.height)
                && Objects.equals(width, that.width)
                && Objects.equals(mediaSize, that.mediaSize)
                && favorite == that.favorite
                && Objects.equals(mediaUrl, that.mediaUrl)
                && Objects.deepEquals(fileEncSha256, that.fileEncSha256)
                && Objects.deepEquals(mediaKey, that.mediaKey)
                && Objects.equals(mimetype, that.mimetype)
                && Objects.equals(mediaDirectPath, that.mediaDirectPath)
                && Objects.equals(deviceIdHint, that.deviceIdHint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaUrl, Arrays.hashCode(fileEncSha256), Arrays.hashCode(mediaKey), mimetype, height, width, mediaDirectPath, mediaSize, favorite, deviceIdHint);
    }

    @Override
    public String toString() {
        return "StickerAction[" +
                "url=" + mediaUrl + ", " +
                "fileEncSha256=" + Arrays.toString(fileEncSha256) + ", " +
                "mediaKey=" + Arrays.toString(mediaKey) + ", " +
                "mimetype=" + mimetype + ", " +
                "height=" + height + ", " +
                "width=" + width + ", " +
                "directPath=" + mediaDirectPath + ", " +
                "mediaSize=" + mediaSize + ", " +
                "favorite=" + favorite + ", " +
                "deviceIdHint=" + deviceIdHint + ']';
    }
}
