package com.github.auties00.cobalt.wire.linked.preference;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * Represents a sticker entry stored in the user's sticker collection.
 *
 * <p>Stickers in WhatsApp are encrypted media items that the user can send in
 * chats. This class models a sticker as it appears in the locally-kept sticker
 * collection (recent stickers, favourites and avatar stickers), carrying all
 * of the media descriptors required to download and decrypt the sticker on
 * demand together with the metadata the UI needs to display it.
 *
 * <p>As a {@link MediaProvider} the sticker exposes the direct path, media
 * URL, encryption key and SHA-256 of the encrypted payload used by the media
 * download pipeline. Dimensions ({@code width} and {@code height}) and MIME
 * type are provided for layout and decoding, while the {@code favorite} and
 * {@code isAvatar} flags discriminate between regular, starred and avatar
 * stickers. The {@code timestamp} records when the sticker was last used and
 * is consulted when sorting the recent stickers list.
 *
 * <p>Equality and hashing take all fields into account, with byte arrays
 * compared by content so that two sticker entries backed by the same encrypted
 * payload are considered equal.
 */
@ProtobufMessage
public final class Sticker implements MediaProvider {
    /**
     * The CDN URL from which the encrypted sticker payload can be downloaded,
     * or {@code null} when the URL has not been resolved yet.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    /**
     * The SHA-256 of the encrypted sticker payload, or {@code null} when it
     * has not been computed yet.
     *
     * <p>Used to verify the integrity of the downloaded bytes before
     * decryption.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * The media key used to derive the encryption and authentication keys for
     * the sticker, or {@code null} when the sticker has not been uploaded
     * through the media pipeline.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * The MIME type of the sticker, typically {@code "image/webp"}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String mimetype;

    /**
     * The height of the sticker in pixels, or {@code null} when unknown.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    final Integer height;

    /**
     * The width of the sticker in pixels, or {@code null} when unknown.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    final Integer width;

    /**
     * The CDN direct path of the sticker payload, or {@code null} when not yet
     * resolved.
     *
     * <p>The direct path is a stable, server-relative address that does not
     * embed authentication tokens and is preferred over {@link #mediaUrl} for
     * long-term storage.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * The size in bytes of the encrypted sticker payload, or {@code null} when
     * unknown.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * Whether the user has marked this sticker as a favourite.
     *
     * <p>Favourite stickers are surfaced in a dedicated section of the sticker
     * picker.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    final boolean favorite;

    /**
     * A hint that records which device identifier was associated with the
     * sticker at the time it was saved, or {@code null} when no hint is
     * available.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
    final Integer deviceIdHint;

    /**
     * The Unix timestamp (in seconds) recording when the sticker was last used
     * or added, or {@code null} when unknown.
     *
     * <p>Used to sort the recent stickers list.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.UINT64)
    Long timestamp;

    /**
     * Whether this sticker belongs to the user's avatar sticker pack.
     *
     * <p>Avatar stickers are auto-generated from the user's avatar and are
     * managed separately from regular stickers.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    final boolean isAvatar;

    /**
     * Constructs a new sticker with the given field values.
     *
     * <p>This constructor is package-private. Application code should obtain
     * instances through the generated {@code StickerBuilder}.
     *
     * @param mediaUrl             the CDN download URL, or {@code null}
     * @param mediaEncryptedSha256 the SHA-256 of the encrypted payload, or {@code null}
     * @param mediaKey             the media key, or {@code null}
     * @param mimetype             the MIME type
     * @param height               the height in pixels, or {@code null}
     * @param width                the width in pixels, or {@code null}
     * @param mediaDirectPath      the CDN direct path, or {@code null}
     * @param mediaSize            the encrypted payload size in bytes, or {@code null}
     * @param favorite             whether the sticker is a favourite
     * @param deviceIdHint         the saved-device hint, or {@code null}
     * @param timestamp            the last-used timestamp in seconds, or {@code null}
     * @param isAvatar             whether the sticker belongs to the avatar pack
     */
    Sticker(String mediaUrl, byte[] mediaEncryptedSha256, byte[] mediaKey, String mimetype, Integer height, Integer width, String mediaDirectPath, Long mediaSize, boolean favorite, Integer deviceIdHint, Long timestamp, boolean isAvatar) {
        this.mediaUrl = mediaUrl;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaKey = mediaKey;
        this.mimetype = mimetype;
        this.height = height;
        this.width = width;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaSize = mediaSize;
        this.favorite = favorite;
        this.deviceIdHint = deviceIdHint;
        this.timestamp = timestamp;
        this.isAvatar = isAvatar;
    }

    /**
     * Returns the CDN download URL of the encrypted sticker payload.
     *
     * @return an {@link Optional} holding the URL, or empty when it has not
     *         been resolved yet
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the SHA-256 of the encrypted sticker payload.
     *
     * @return an {@link Optional} holding the SHA-256 bytes, or empty when not
     *         available
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the media key used to decrypt this sticker.
     *
     * @return an {@link Optional} holding the media key, or empty when the
     *         sticker has not been uploaded through the media pipeline
     */
    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the MIME type of the sticker.
     *
     * @return an {@link Optional} holding the MIME type, or empty when unknown
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the height of the sticker in pixels.
     *
     * @return an {@link OptionalInt} holding the height, or empty when unknown
     */
    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    /**
     * Returns the width of the sticker in pixels.
     *
     * @return an {@link OptionalInt} holding the width, or empty when unknown
     */
    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the CDN direct path of the sticker payload.
     *
     * @return an {@link Optional} holding the direct path, or empty when not
     *         available
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the size in bytes of the encrypted sticker payload.
     *
     * @return an {@link OptionalLong} holding the size, or empty when unknown
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns whether the user has marked this sticker as a favourite.
     *
     * @return {@code true} when the sticker is a favourite, {@code false}
     *         otherwise
     */
    public boolean favorite() {
        return favorite;
    }

    /**
     * Returns the device identifier hint recorded when the sticker was saved.
     *
     * @return an {@link OptionalInt} holding the device identifier, or empty
     *         when no hint is available
     */
    public OptionalInt deviceIdHint() {
        return deviceIdHint == null ? OptionalInt.empty() : OptionalInt.of(deviceIdHint);
    }

    /**
     * Returns the Unix timestamp (in seconds) recording when the sticker was
     * last used or added.
     *
     * @return an {@link OptionalLong} holding the timestamp, or empty when
     *         unknown
     */
    public OptionalLong timestamp() {
        return timestamp == null ? OptionalLong.empty() : OptionalLong.of(timestamp);
    }

    /**
     * Returns whether this sticker belongs to the user's avatar sticker pack.
     *
     * @return {@code true} when the sticker is an avatar sticker, {@code false}
     *         otherwise
     */
    public boolean isAvatar() {
        return isAvatar;
    }

    /**
     * Returns the SHA-256 of the decrypted sticker payload.
     *
     * <p>Stickers never carry the plaintext SHA-256 in Cobalt's collection
     * model, so this method always returns an empty {@link Optional}.
     *
     * @return an empty {@link Optional}
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.empty();
    }

    /**
     * Returns the media path category used by the download pipeline to reach
     * sticker CDN endpoints.
     *
     * @return {@link MediaPath#STICKER}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.STICKER;
    }

    /**
     * Updates the CDN download URL of the encrypted sticker payload.
     *
     * @param mediaUrl the new URL, or {@code null} to clear it
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Updates the SHA-256 of the encrypted sticker payload.
     *
     * @param mediaEncryptedSha256 the new SHA-256 bytes, or {@code null} to
     *                             clear it
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Updates the media key used to decrypt this sticker.
     *
     * @param mediaKey the new media key, or {@code null} to clear it
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Updates the CDN direct path of the sticker payload.
     *
     * @param mediaDirectPath the new direct path, or {@code null} to clear it
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    /**
     * Updates the size in bytes of the encrypted sticker payload.
     *
     * @param mediaSize the new size in bytes
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Updates the last-used timestamp of this sticker.
     *
     * @param timestamp the new Unix timestamp in seconds, or {@code null} to
     *                  clear it
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Updates the SHA-256 of the decrypted sticker payload.
     *
     * <p>Stickers never store the plaintext SHA-256 in Cobalt's collection
     * model, so this method is a no-op. It exists to satisfy the
     * {@link MediaProvider} contract.
     *
     * @param bytes ignored
     */
    @Override
    public void setMediaSha256(byte[] bytes) {
    }

    /**
     * Updates the media key rotation timestamp.
     *
     * <p>Sticker entries do not track media key rotation timestamps, so this
     * method is a no-op. It exists to satisfy the {@link MediaProvider}
     * contract.
     *
     * @param timestamp ignored
     */
    @Override
    public void setMediaKeyTimestamp(Instant timestamp) {
    }

    /**
     * Returns whether this sticker equals the given object.
     *
     * <p>Two stickers are equal when every descriptor field matches, with byte
     * arrays compared by content rather than by reference.
     *
     * @param o the object to compare against
     * @return {@code true} when the object is a {@link Sticker} with matching
     *         field values, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Sticker that
               && Objects.equals(height, that.height)
               && Objects.equals(width, that.width)
               && Objects.equals(mediaSize, that.mediaSize)
               && favorite == that.favorite
               && isAvatar == that.isAvatar
               && Objects.equals(mediaUrl, that.mediaUrl)
               && Objects.deepEquals(mediaEncryptedSha256, that.mediaEncryptedSha256)
               && Objects.deepEquals(mediaKey, that.mediaKey)
               && Objects.equals(mimetype, that.mimetype)
               && Objects.equals(mediaDirectPath, that.mediaDirectPath)
               && Objects.equals(deviceIdHint, that.deviceIdHint)
               && Objects.equals(timestamp, that.timestamp);
    }

    /**
     * Returns the hash code of this sticker, consistent with
     * {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(mediaUrl, Arrays.hashCode(mediaEncryptedSha256), Arrays.hashCode(mediaKey), mimetype, height, width, mediaDirectPath, mediaSize, favorite, deviceIdHint, timestamp, isAvatar);
    }

    /**
     * Returns a human-readable string containing every sticker field.
     *
     * <p>Byte arrays are rendered through {@link Arrays#toString(byte[])} so
     * that their contents are visible in logs.
     *
     * @return the string representation of this sticker
     */
    @Override
    public String toString() {
        return "Sticker[" +
               "url=" + mediaUrl + ", " +
               "fileEncSha256=" + Arrays.toString(mediaEncryptedSha256) + ", " +
               "mediaKey=" + Arrays.toString(mediaKey) + ", " +
               "mimetype=" + mimetype + ", " +
               "height=" + height + ", " +
               "width=" + width + ", " +
               "directPath=" + mediaDirectPath + ", " +
               "mediaSize=" + mediaSize + ", " +
               "favorite=" + favorite + ", " +
               "deviceIdHint=" + deviceIdHint + ", " +
               "timestamp=" + timestamp + ", " +
               "isAvatar=" + isAvatar + ']';
    }
}
