package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Metadata describing an externally hosted thumbnail for media messages delivered
 * over the MMS CDN.
 *
 * <p>Instead of embedding a JPEG preview directly into the message, WhatsApp can
 * reference a thumbnail stored on the MMS servers. This class carries the CDN path,
 * integrity hashes, decryption key and dimensions required to download and render
 * the thumbnail securely.
 */
@ProtobufMessage(name = "Message.MMSThumbnailMetadata")
public final class MessageMMSThumbnailMetadata implements Message {
    /**
     * CDN direct path to the encrypted thumbnail.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String thumbnailDirectPath;

    /**
     * SHA-256 digest of the decrypted thumbnail bytes.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] thumbnailSha256;

    /**
     * SHA-256 digest of the encrypted thumbnail bytes.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] thumbnailEncSha256;

    /**
     * Symmetric key used to decrypt the thumbnail.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * Moment at which the {@link #mediaKey} was generated.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * Thumbnail height in pixels.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer thumbnailHeight;

    /**
     * Thumbnail width in pixels.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    Integer thumbnailWidth;

    /**
     * Domain identifier that scopes how the {@link #mediaKey} was derived.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    MediaMessageKeyDomain mediaKeyDomain;


    /**
     * Constructs a new MMS thumbnail metadata record.
     *
     * @param thumbnailDirectPath the CDN path of the encrypted thumbnail
     * @param thumbnailSha256     the hash of the decrypted thumbnail
     * @param thumbnailEncSha256  the hash of the encrypted thumbnail
     * @param mediaKey            the decryption key
     * @param mediaKeyTimestamp   the moment the key was generated
     * @param thumbnailHeight     the height in pixels
     * @param thumbnailWidth      the width in pixels
     * @param mediaKeyDomain      the key derivation domain
     */
    MessageMMSThumbnailMetadata(String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, byte[] mediaKey, Instant mediaKeyTimestamp, Integer thumbnailHeight, Integer thumbnailWidth, MediaMessageKeyDomain mediaKeyDomain) {
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.mediaKey = mediaKey;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailWidth = thumbnailWidth;
        this.mediaKeyDomain = mediaKeyDomain;
    }

    /**
     * Returns the CDN direct path of the encrypted thumbnail.
     *
     * @return the path, or empty if unset
     */
    public Optional<String> thumbnailDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    /**
     * Returns the SHA-256 digest of the decrypted thumbnail.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> thumbnailSha256() {
        return Optional.ofNullable(thumbnailSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted thumbnail.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> thumbnailEncSha256() {
        return Optional.ofNullable(thumbnailEncSha256);
    }

    /**
     * Returns the symmetric key used to decrypt the thumbnail.
     *
     * @return the key, or empty if unset
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
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
     * Returns the domain identifier that scopes how the media key was derived.
     *
     * @return the key domain, or empty if unset
     */
    public Optional<MediaMessageKeyDomain> mediaKeyDomain() {
        return Optional.ofNullable(mediaKeyDomain);
    }

    /**
     * Updates the CDN direct path of the encrypted thumbnail.
     *
     * @param thumbnailDirectPath the new path, or {@code null} to clear
     */
    public void setThumbnailDirectPath(String thumbnailDirectPath) {
        this.thumbnailDirectPath = thumbnailDirectPath;
    }

    /**
     * Updates the hash of the decrypted thumbnail.
     *
     * @param thumbnailSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailSha256(byte[] thumbnailSha256) {
        this.thumbnailSha256 = thumbnailSha256;
    }

    /**
     * Updates the hash of the encrypted thumbnail.
     *
     * @param thumbnailEncSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailEncSha256(byte[] thumbnailEncSha256) {
        this.thumbnailEncSha256 = thumbnailEncSha256;
    }

    /**
     * Updates the symmetric decryption key.
     *
     * @param mediaKey the new key, or {@code null} to clear
     */
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
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
     * Updates the key derivation domain.
     *
     * @param mediaKeyDomain the new domain, or {@code null} to clear
     */
    public void setMediaKeyDomain(MediaMessageKeyDomain mediaKeyDomain) {
        this.mediaKeyDomain = mediaKeyDomain;
    }
}
