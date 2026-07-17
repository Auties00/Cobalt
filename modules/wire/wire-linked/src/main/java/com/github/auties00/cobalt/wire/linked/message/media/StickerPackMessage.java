package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * A message that shares an entire sticker pack with the recipient.
 *
 * <p>Sticker packs are collections of individual stickers that can be installed
 * by a user on their device. This message carries the pack's identity fields
 * (id, name, publisher, description), the list of {@link Sticker} descriptors
 * making up the pack, the encrypted bundle's CDN metadata and the tray icon used
 * in the sticker selector UI.
 */
@ProtobufMessage(name = "Message.StickerPackMessage")
public final class StickerPackMessage implements ContextualMessage {
    /**
     * Unique identifier of the sticker pack.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String stickerPackId;

    /**
     * Display name of the sticker pack.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * Name of the entity that published the sticker pack.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String publisher;

    /**
     * Descriptors of the individual stickers included in the pack.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    List<Sticker> stickers;

    /**
     * Size in bytes of the decrypted pack bundle.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    Long fileLength;

    /**
     * SHA-256 digest of the decrypted pack bundle.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] fileSha256;

    /**
     * SHA-256 digest of the encrypted pack bundle.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] fileEncSha256;

    /**
     * Symmetric key used to decrypt the pack bundle.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * CDN direct path of the encrypted pack bundle.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String directPath;

    /**
     * Optional caption shown when sharing the pack.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String caption;

    /**
     * Contextual information attached to the pack, such as a quoted message or
     * forwarding metadata.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Longer descriptive text shown on the pack's details screen.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    String packDescription;

    /**
     * Moment at which the {@link #mediaKey} was generated.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * File name of the tray icon inside the pack bundle.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    String trayIconFileName;

    /**
     * CDN direct path of the encrypted tray thumbnail image.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    String thumbnailDirectPath;

    /**
     * SHA-256 digest of the decrypted tray thumbnail.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    byte[] thumbnailSha256;

    /**
     * SHA-256 digest of the encrypted tray thumbnail.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.BYTES)
    byte[] thumbnailEncSha256;

    /**
     * Thumbnail height in pixels.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.UINT32)
    Integer thumbnailHeight;

    /**
     * Thumbnail width in pixels.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.UINT32)
    Integer thumbnailWidth;

    /**
     * Hash used by clients to deduplicate pack preview images.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    String imageDataHash;

    /**
     * Total installed size of the pack in bytes.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.UINT64)
    Long stickerPackSize;

    /**
     * Origin of the pack: first-party, third-party or user-created.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.ENUM)
    StickerPackOrigin stickerPackOrigin;


    /**
     * Constructs a new sticker pack message with the given metadata.
     *
     * @param stickerPackId       the pack identifier
     * @param name                the display name
     * @param publisher           the publisher name
     * @param stickers            the individual sticker descriptors
     * @param fileLength          the decrypted bundle size
     * @param fileSha256          the decrypted bundle hash
     * @param fileEncSha256       the encrypted bundle hash
     * @param mediaKey            the bundle decryption key
     * @param directPath          the bundle CDN path
     * @param caption             the caption shown when sharing
     * @param contextInfo         the context information
     * @param packDescription     the long-form description
     * @param mediaKeyTimestamp   when the key was generated
     * @param trayIconFileName    the tray icon file name
     * @param thumbnailDirectPath the tray thumbnail CDN path
     * @param thumbnailSha256     the decrypted thumbnail hash
     * @param thumbnailEncSha256  the encrypted thumbnail hash
     * @param thumbnailHeight     the thumbnail height
     * @param thumbnailWidth      the thumbnail width
     * @param imageDataHash       the image deduplication hash
     * @param stickerPackSize     the installed pack size
     * @param stickerPackOrigin   the pack origin
     */
    StickerPackMessage(String stickerPackId, String name, String publisher, List<Sticker> stickers, Long fileLength, byte[] fileSha256, byte[] fileEncSha256, byte[] mediaKey, String directPath, String caption, ContextInfo contextInfo, String packDescription, Instant mediaKeyTimestamp, String trayIconFileName, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, Integer thumbnailHeight, Integer thumbnailWidth, String imageDataHash, Long stickerPackSize, StickerPackOrigin stickerPackOrigin) {
        this.stickerPackId = stickerPackId;
        this.name = name;
        this.publisher = publisher;
        this.stickers = stickers;
        this.fileLength = fileLength;
        this.fileSha256 = fileSha256;
        this.fileEncSha256 = fileEncSha256;
        this.mediaKey = mediaKey;
        this.directPath = directPath;
        this.caption = caption;
        this.contextInfo = contextInfo;
        this.packDescription = packDescription;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.trayIconFileName = trayIconFileName;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailWidth = thumbnailWidth;
        this.imageDataHash = imageDataHash;
        this.stickerPackSize = stickerPackSize;
        this.stickerPackOrigin = stickerPackOrigin;
    }

    /**
     * Returns the unique sticker pack identifier.
     *
     * @return the identifier, or empty if unset
     */
    public Optional<String> stickerPackId() {
        return Optional.ofNullable(stickerPackId);
    }

    /**
     * Returns the display name of the pack.
     *
     * @return the name, or empty if unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the publisher name.
     *
     * @return the publisher, or empty if unset
     */
    public Optional<String> publisher() {
        return Optional.ofNullable(publisher);
    }

    /**
     * Returns the individual sticker descriptors that make up the pack.
     *
     * @return an unmodifiable list, empty if no stickers are declared
     */
    public List<Sticker> stickers() {
        return stickers == null ? List.of() : Collections.unmodifiableList(stickers);
    }

    /**
     * Returns the size in bytes of the decrypted pack bundle.
     *
     * @return the size, or empty if unknown
     */
    public OptionalLong fileLength() {
        return fileLength == null ? OptionalLong.empty() : OptionalLong.of(fileLength);
    }

    /**
     * Returns the SHA-256 digest of the decrypted pack bundle.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(fileSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted pack bundle.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(fileEncSha256);
    }

    /**
     * Returns the symmetric key used to decrypt the pack bundle.
     *
     * @return the key, or empty if unset
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the CDN direct path of the encrypted pack bundle.
     *
     * @return the path, or empty if unset
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(directPath);
    }

    /**
     * Returns the caption shown when sharing the pack.
     *
     * @return the caption, or empty if unset
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the context information attached to this pack message.
     *
     * @return the context info, or empty if none is attached
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the long-form pack description.
     *
     * @return the description, or empty if unset
     */
    public Optional<String> packDescription() {
        return Optional.ofNullable(packDescription);
    }

    /**
     * Returns the moment at which the media key was generated.
     *
     * @return the timestamp, or empty if unknown
     */
    public Optional<Instant> mediaKeyTimestamp() {
        return Optional.ofNullable(mediaKeyTimestamp);
    }

    /**
     * Returns the tray icon file name inside the bundle.
     *
     * @return the file name, or empty if unset
     */
    public Optional<String> trayIconFileName() {
        return Optional.ofNullable(trayIconFileName);
    }

    /**
     * Returns the CDN direct path of the tray thumbnail image.
     *
     * @return the path, or empty if unset
     */
    public Optional<String> thumbnailDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    /**
     * Returns the SHA-256 digest of the decrypted tray thumbnail.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> thumbnailSha256() {
        return Optional.ofNullable(thumbnailSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted tray thumbnail.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> thumbnailEncSha256() {
        return Optional.ofNullable(thumbnailEncSha256);
    }

    /**
     * Returns the thumbnail height in pixels.
     *
     * @return the height, or empty if unknown
     */
    public OptionalInt thumbnailHeight() {
        return thumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbnailHeight);
    }

    /**
     * Returns the thumbnail width in pixels.
     *
     * @return the width, or empty if unknown
     */
    public OptionalInt thumbnailWidth() {
        return thumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbnailWidth);
    }

    /**
     * Returns the image data deduplication hash.
     *
     * @return the hash, or empty if unset
     */
    public Optional<String> imageDataHash() {
        return Optional.ofNullable(imageDataHash);
    }

    /**
     * Returns the installed size of the pack in bytes.
     *
     * @return the size, or empty if unknown
     */
    public OptionalLong stickerPackSize() {
        return stickerPackSize == null ? OptionalLong.empty() : OptionalLong.of(stickerPackSize);
    }

    /**
     * Returns the origin of the pack.
     *
     * @return the origin, or empty if unset
     */
    public Optional<StickerPackOrigin> stickerPackOrigin() {
        return Optional.ofNullable(stickerPackOrigin);
    }

    /**
     * Updates the pack identifier.
     *
     * @param stickerPackId the new identifier, or {@code null} to clear
     */
    public void setStickerPackId(String stickerPackId) {
        this.stickerPackId = stickerPackId;
    }

    /**
     * Updates the display name.
     *
     * @param name the new name, or {@code null} to clear
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Updates the publisher name.
     *
     * @param publisher the new publisher, or {@code null} to clear
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * Updates the sticker descriptors.
     *
     * @param stickers the new list, or {@code null} to clear
     */
    public void setStickers(List<Sticker> stickers) {
        this.stickers = stickers;
    }

    /**
     * Updates the decrypted bundle size.
     *
     * @param fileLength the new size, or {@code null} to clear
     */
    public void setFileLength(Long fileLength) {
        this.fileLength = fileLength;
    }

    /**
     * Updates the decrypted bundle hash.
     *
     * @param fileSha256 the new hash, or {@code null} to clear
     */
    public void setFileSha256(byte[] fileSha256) {
        this.fileSha256 = fileSha256;
    }

    /**
     * Updates the encrypted bundle hash.
     *
     * @param fileEncSha256 the new hash, or {@code null} to clear
     */
    public void setFileEncSha256(byte[] fileEncSha256) {
        this.fileEncSha256 = fileEncSha256;
    }

    /**
     * Updates the bundle decryption key.
     *
     * @param mediaKey the new key, or {@code null} to clear
     */
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Updates the bundle CDN path.
     *
     * @param directPath the new path, or {@code null} to clear
     */
    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    /**
     * Updates the caption.
     *
     * @param caption the new caption, or {@code null} to clear
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Updates the context information attached to this pack message.
     *
     * @param contextInfo the new context info, or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Updates the pack description.
     *
     * @param packDescription the new description, or {@code null} to clear
     */
    public void setPackDescription(String packDescription) {
        this.packDescription = packDescription;
    }

    /**
     * Updates the moment at which the media key was generated.
     *
     * @param mediaKeyTimestamp the new timestamp, or {@code null} to clear
     */
    public void setMediaKeyTimestamp(Instant mediaKeyTimestamp) {
        this.mediaKeyTimestamp = mediaKeyTimestamp;
    }

    /**
     * Updates the tray icon file name.
     *
     * @param trayIconFileName the new file name, or {@code null} to clear
     */
    public void setTrayIconFileName(String trayIconFileName) {
        this.trayIconFileName = trayIconFileName;
    }

    /**
     * Updates the tray thumbnail CDN path.
     *
     * @param thumbnailDirectPath the new path, or {@code null} to clear
     */
    public void setThumbnailDirectPath(String thumbnailDirectPath) {
        this.thumbnailDirectPath = thumbnailDirectPath;
    }

    /**
     * Updates the decrypted thumbnail hash.
     *
     * @param thumbnailSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailSha256(byte[] thumbnailSha256) {
        this.thumbnailSha256 = thumbnailSha256;
    }

    /**
     * Updates the encrypted thumbnail hash.
     *
     * @param thumbnailEncSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailEncSha256(byte[] thumbnailEncSha256) {
        this.thumbnailEncSha256 = thumbnailEncSha256;
    }

    /**
     * Updates the thumbnail height.
     *
     * @param thumbnailHeight the new height in pixels, or {@code null} to clear
     */
    public void setThumbnailHeight(Integer thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }

    /**
     * Updates the thumbnail width.
     *
     * @param thumbnailWidth the new width in pixels, or {@code null} to clear
     */
    public void setThumbnailWidth(Integer thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    /**
     * Updates the image deduplication hash.
     *
     * @param imageDataHash the new hash, or {@code null} to clear
     */
    public void setImageDataHash(String imageDataHash) {
        this.imageDataHash = imageDataHash;
    }

    /**
     * Updates the installed pack size.
     *
     * @param stickerPackSize the new size, or {@code null} to clear
     */
    public void setStickerPackSize(Long stickerPackSize) {
        this.stickerPackSize = stickerPackSize;
    }

    /**
     * Updates the pack origin.
     *
     * @param stickerPackOrigin the new origin, or {@code null} to clear
     */
    public void setStickerPackOrigin(StickerPackOrigin stickerPackOrigin) {
        this.stickerPackOrigin = stickerPackOrigin;
    }

    /**
     * Origin of a shared sticker pack.
     *
     * <p>Indicates whether the pack was shipped by WhatsApp itself, obtained from a
     * third-party catalog, or created by the user themselves.
     */
    @ProtobufEnum(name = "Message.StickerPackMessage.StickerPackOrigin")
    public static enum StickerPackOrigin {
        /**
         * Pack shipped directly by WhatsApp.
         */
        FIRST_PARTY(0),
        /**
         * Pack sourced from a third-party catalog.
         */
        THIRD_PARTY(1),
        /**
         * Pack authored by the user.
         */
        USER_CREATED(2);

        /**
         * Constructs a new enum constant.
         *
         * @param index the protobuf wire index used to serialize this constant
         */
        StickerPackOrigin(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index of this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Descriptor of an individual sticker contained inside a {@link StickerPackMessage}.
     *
     * <p>A descriptor carries only naming and tagging information: the actual sticker
     * image bytes are delivered as part of the encrypted pack bundle referenced by
     * the outer message.
     */
    @ProtobufMessage(name = "Message.StickerPackMessage.Sticker")
    public static final class Sticker {
        /**
         * File name of the sticker inside the pack bundle.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String fileName;

        /**
         * Whether this sticker is animated.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
        Boolean isAnimated;

        /**
         * Emoji tags associated with the sticker, used for search and suggestion.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        List<String> emojis;

        /**
         * Accessibility label describing the sticker for screen readers.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String accessibilityLabel;

        /**
         * Whether this sticker uses the Lottie animation format.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
        Boolean isLottie;

        /**
         * MIME type of the sticker image.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String mimetype;


        /**
         * Constructs a new sticker descriptor.
         *
         * @param fileName           the file name within the bundle
         * @param isAnimated         whether the sticker is animated
         * @param emojis             the emoji tags
         * @param accessibilityLabel the accessibility description
         * @param isLottie           whether the sticker is a Lottie animation
         * @param mimetype           the image MIME type
         */
        Sticker(String fileName, Boolean isAnimated, List<String> emojis, String accessibilityLabel, Boolean isLottie, String mimetype) {
            this.fileName = fileName;
            this.isAnimated = isAnimated;
            this.emojis = emojis;
            this.accessibilityLabel = accessibilityLabel;
            this.isLottie = isLottie;
            this.mimetype = mimetype;
        }

        /**
         * Returns the file name of the sticker inside the bundle.
         *
         * @return the file name, or empty if unset
         */
        public Optional<String> fileName() {
            return Optional.ofNullable(fileName);
        }

        /**
         * Returns whether this sticker is animated.
         *
         * @return {@code true} if animated
         */
        public boolean isAnimated() {
            return isAnimated != null && isAnimated;
        }

        /**
         * Returns the emoji tags associated with the sticker.
         *
         * @return an unmodifiable list, empty if no emojis are set
         */
        public List<String> emojis() {
            return emojis == null ? List.of() : Collections.unmodifiableList(emojis);
        }

        /**
         * Returns the accessibility label.
         *
         * @return the label, or empty if not provided
         */
        public Optional<String> accessibilityLabel() {
            return Optional.ofNullable(accessibilityLabel);
        }

        /**
         * Returns whether this sticker uses the Lottie animation format.
         *
         * @return {@code true} if this is a Lottie sticker
         */
        public boolean isLottie() {
            return isLottie != null && isLottie;
        }

        /**
         * Returns the MIME type of the sticker image.
         *
         * @return the MIME type, or empty if unset
         */
        public Optional<String> mimetype() {
            return Optional.ofNullable(mimetype);
        }

        /**
         * Updates the file name.
         *
         * @param fileName the new file name, or {@code null} to clear
         */
        public void setFileName(String fileName) {
            this.fileName = fileName;
    }

        /**
         * Updates the animated flag.
         *
         * @param isAnimated {@code true} if animated, {@code false} or {@code null} otherwise
         */
        public void setAnimated(Boolean isAnimated) {
            this.isAnimated = isAnimated;
    }

        /**
         * Updates the emoji tags.
         *
         * @param emojis the new tags, or {@code null} to clear
         */
        public void setEmojis(List<String> emojis) {
            this.emojis = emojis;
    }

        /**
         * Updates the accessibility label.
         *
         * @param accessibilityLabel the new label, or {@code null} to clear
         */
        public void setAccessibilityLabel(String accessibilityLabel) {
            this.accessibilityLabel = accessibilityLabel;
    }

        /**
         * Updates the Lottie format flag.
         *
         * @param isLottie {@code true} for Lottie, {@code false} or {@code null} otherwise
         */
        public void setLottie(Boolean isLottie) {
            this.isLottie = isLottie;
    }

        /**
         * Updates the MIME type.
         *
         * @param mimetype the new MIME type, or {@code null} to clear
         */
        public void setMimetype(String mimetype) {
            this.mimetype = mimetype;
    }
    }
}
