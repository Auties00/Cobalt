package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.preference.Sticker;
import com.github.auties00.cobalt.wire.linked.preference.StickerBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A sync action that propagates favorite-sticker changes across linked devices
 * and carries the full sticker media descriptor needed to re-download the
 * sticker on any device.
 *
 * <p>The action doubles as a {@link MediaProvider} because favoriting a
 * sticker requires every linked device to be able to fetch the sticker binary
 * from the CDN. Implementations therefore include the encrypted hash, media
 * key, direct path, mime type, dimensions, file size, and the media key
 * generation timestamp used by the CDN to validate key freshness.
 *
 * <p>Additional flags describe how the sticker should be interpreted: whether
 * it is a favorite, whether it is a Lottie animation, and whether it is an
 * avatar sticker. The action also records a {@code deviceIdHint} and an
 * {@code imageHash} to help consumers deduplicate sticker variants.
 */
@ProtobufMessage(name = "SyncActionValue.StickerAction")
public final class StickerAction implements SyncAction<StickerActionArgs>, MediaProvider {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "favoriteSticker";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * The app-state collection that stores this action type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the action name used to route this action through the app-state
     * sync pipeline.
     *
     * @return the canonical action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version used to route this action through the
     * app-state sync pipeline.
     *
     * @return the canonical action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The CDN URL from which the sticker binary can be downloaded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    /**
     * The SHA-256 hash of the encrypted sticker payload, used to verify the
     * downloaded bytes.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * The symmetric media key used to decrypt the sticker payload.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * The MIME type of the sticker binary.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String mimetype;

    /**
     * The sticker image height in pixels.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer height;

    /**
     * The sticker image width in pixels.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer width;

    /**
     * The CDN direct path used to re-resolve the sticker URL when it expires.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * The size of the encrypted sticker payload in bytes.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * Whether the sticker is currently marked as a favorite.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
    Boolean isFavorite;

    /**
     * A hint identifying which of the user's devices originally received the
     * sticker. Used to disambiguate sticker variants across devices.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
    Integer deviceIdHint;

    /**
     * Whether the sticker is a Lottie animation rather than a static image.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    Boolean isLottie;

    /**
     * A content hash used to deduplicate visually identical stickers.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    String imageHash;

    /**
     * Whether the sticker is an avatar sticker generated from the user's
     * personalised avatar.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    Boolean isAvatarSticker;

    /**
     * The timestamp at which the sticker's media key was generated. Used by
     * the CDN to validate key freshness on subsequent downloads.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;


    /**
     * Constructs a new {@code StickerAction} carrying the full sticker media
     * descriptor and associated flags.
     *
     * @param mediaUrl             the CDN URL of the sticker
     * @param mediaEncryptedSha256 the SHA-256 of the encrypted payload
     * @param mediaKey             the symmetric decryption key
     * @param mimetype             the MIME type of the sticker binary
     * @param height               the sticker image height in pixels
     * @param width                the sticker image width in pixels
     * @param mediaDirectPath      the CDN direct path for URL re-resolution
     * @param mediaSize            the size of the encrypted payload in bytes
     * @param isFavorite           whether the sticker is marked as favorite
     * @param deviceIdHint         a hint identifying the originating device
     * @param isLottie             whether the sticker is a Lottie animation
     * @param imageHash            the content hash for deduplication
     * @param isAvatarSticker      whether this is an avatar sticker
     * @param mediaKeyTimestamp    the media key generation timestamp
     */
    StickerAction(String mediaUrl, byte[] mediaEncryptedSha256, byte[] mediaKey, String mimetype, Integer height, Integer width, String mediaDirectPath, Long mediaSize, Boolean isFavorite, Integer deviceIdHint, Boolean isLottie, String imageHash, Boolean isAvatarSticker, Instant mediaKeyTimestamp) {
        this.mediaUrl = mediaUrl;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaKey = mediaKey;
        this.mimetype = mimetype;
        this.height = height;
        this.width = width;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaSize = mediaSize;
        this.isFavorite = isFavorite;
        this.deviceIdHint = deviceIdHint;
        this.isLottie = isLottie;
        this.imageHash = imageHash;
        this.isAvatarSticker = isAvatarSticker;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
    }

    /**
     * Returns the CDN URL from which the sticker binary can be downloaded.
     *
     * @return the CDN URL, or {@link Optional#empty()} if unset
     */
    public Optional<String> url() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the CDN URL from which the sticker binary can be downloaded.
     *
     * @return the CDN URL, or {@link Optional#empty()} if unset
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the SHA-256 hash of the encrypted sticker payload.
     *
     * @return the encrypted-payload hash, or {@link Optional#empty()} if unset
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the SHA-256 hash of the encrypted sticker payload.
     *
     * @return the encrypted-payload hash, or {@link Optional#empty()} if unset
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the symmetric media key used to decrypt the sticker payload.
     *
     * @return the media key, or {@link Optional#empty()} if unset
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the MIME type of the sticker binary.
     *
     * @return the MIME type, or {@link Optional#empty()} if unset
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the sticker image height in pixels.
     *
     * @return the height, or {@link OptionalInt#empty()} if unset
     */
    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    /**
     * Returns the sticker image width in pixels.
     *
     * @return the width, or {@link OptionalInt#empty()} if unset
     */
    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the CDN direct path used to re-resolve the sticker URL when it
     * expires.
     *
     * @return the direct path, or {@link Optional#empty()} if unset
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the CDN direct path used to re-resolve the sticker URL when it
     * expires.
     *
     * @return the direct path, or {@link Optional#empty()} if unset
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the size of the encrypted sticker payload in bytes.
     *
     * @return the payload size in bytes, or {@link OptionalLong#empty()} if unset
     */
    public OptionalLong fileLength() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the size of the encrypted sticker payload in bytes.
     *
     * @return the payload size in bytes, or {@link OptionalLong#empty()} if unset
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns whether the sticker is currently marked as a favorite.
     *
     * @return {@code true} if the sticker is a favorite, {@code false} otherwise
     */
    public boolean isFavorite() {
        return isFavorite != null && isFavorite;
    }

    /**
     * Returns the hint identifying the originating device for this sticker.
     *
     * @return the device id hint, or {@link OptionalInt#empty()} if unset
     */
    public OptionalInt deviceIdHint() {
        return deviceIdHint == null ? OptionalInt.empty() : OptionalInt.of(deviceIdHint);
    }

    /**
     * Returns whether the sticker is a Lottie animation rather than a static
     * image.
     *
     * @return {@code true} if the sticker is a Lottie animation, {@code false} otherwise
     */
    public boolean isLottie() {
        return isLottie != null && isLottie;
    }

    /**
     * Returns the content hash used to deduplicate visually identical
     * stickers.
     *
     * @return the image hash, or {@link Optional#empty()} if unset
     */
    public Optional<String> imageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * Returns whether the sticker is an avatar sticker generated from the
     * user's personalised avatar.
     *
     * @return {@code true} if the sticker is an avatar sticker, {@code false} otherwise
     */
    public boolean isAvatarSticker() {
        return isAvatarSticker != null && isAvatarSticker;
    }

    /**
     * Returns the timestamp at which the sticker's media key was generated.
     *
     * @return the media key timestamp, or {@link Optional#empty()} if unset
     */
    public Optional<Instant> mediaKeyTimestamp() {
        return Optional.ofNullable(mediaKeyTimestamp);
    }

    /**
     * Returns the plaintext SHA-256 hash of the sticker payload.
     *
     * <p>The sticker action does not carry a plaintext hash, so this method
     * always returns {@link Optional#empty()}.
     *
     * @return {@link Optional#empty()}
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.empty();
    }

    /**
     * Returns the CDN media path category used to resolve sticker URLs.
     *
     * @return {@link MediaPath#STICKER}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.STICKER;
    }

    /**
     * Sets the CDN URL from which the sticker binary can be downloaded.
     *
     * @param mediaUrl the new CDN URL, or {@code null} to clear it
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Sets the SHA-256 hash of the encrypted sticker payload.
     *
     * @param mediaEncryptedSha256 the new encrypted-payload hash, or
     *                             {@code null} to clear it
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Sets the symmetric media key used to decrypt the sticker payload.
     *
     * @param mediaKey the new media key, or {@code null} to clear it
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Sets the MIME type of the sticker binary.
     *
     * @param mimetype the new MIME type, or {@code null} to clear it
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Sets the sticker image height in pixels.
     *
     * @param height the new height, or {@code null} to clear it
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Sets the sticker image width in pixels.
     *
     * @param width the new width, or {@code null} to clear it
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Sets the CDN direct path used to re-resolve the sticker URL when it
     * expires.
     *
     * @param mediaDirectPath the new direct path, or {@code null} to clear it
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    /**
     * Sets the size of the encrypted sticker payload in bytes.
     *
     * @param mediaSize the new payload size in bytes
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Sets whether the sticker is currently marked as a favorite.
     *
     * @param isFavorite the new favorite flag, or {@code null} to clear it
     */
    public void setFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    /**
     * Sets the hint identifying the originating device for this sticker.
     *
     * @param deviceIdHint the new device id hint, or {@code null} to clear it
     */
    public void setDeviceIdHint(Integer deviceIdHint) {
        this.deviceIdHint = deviceIdHint;
    }

    /**
     * Sets whether the sticker is a Lottie animation.
     *
     * @param isLottie the new Lottie flag, or {@code null} to clear it
     */
    public void setLottie(Boolean isLottie) {
        this.isLottie = isLottie;
    }

    /**
     * Sets the content hash used to deduplicate visually identical stickers.
     *
     * @param imageHash the new image hash, or {@code null} to clear it
     */
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    /**
     * Sets whether the sticker is an avatar sticker generated from the user's
     * personalised avatar.
     *
     * @param isAvatarSticker the new avatar-sticker flag, or {@code null} to clear it
     */
    public void setAvatarSticker(Boolean isAvatarSticker) {
        this.isAvatarSticker = isAvatarSticker;
    }

    /**
     * Sets the plaintext SHA-256 hash of the sticker payload.
     *
     * <p>Sticker actions do not carry a plaintext hash, so this setter is a
     * no-op and exists only to satisfy the {@link MediaProvider} contract.
     *
     * @param bytes ignored
     */
    @Override
    public void setMediaSha256(byte[] bytes) {
    }

    /**
     * Sets the timestamp at which the sticker's media key was generated.
     *
     * @param timestamp the new media key timestamp, or {@code null} to clear it
     */
    @Override
    public void setMediaKeyTimestamp(Instant timestamp) {
        this.mediaKeyTimestamp = timestamp;
    }

    /**
     * Converts this sticker action into a {@link Sticker} preference entry by
     * copying over the media descriptors and feature flags that have a
     * matching representation on {@code Sticker}.
     *
     * <p>The media key timestamp is intentionally dropped by this conversion
     * because {@code Sticker.timestamp} represents a generic sticker
     * timestamp, not the media key timestamp, and the preference model does
     * not currently expose a dedicated field for it.
     *
     * @return a {@link Sticker} populated with the matching fields
     */
    public Sticker toSticker() {
        return new StickerBuilder()
                .mediaUrl(mediaUrl)
                .mediaEncryptedSha256(mediaEncryptedSha256)
                .mediaKey(mediaKey)
                .mimetype(mimetype)
                .height(height)
                .width(width)
                .mediaDirectPath(mediaDirectPath)
                .mediaSize(mediaSize)
                .favorite(isFavorite())
                .deviceIdHint(deviceIdHint)
                .isAvatar(isAvatarSticker())
                .build();
    }
}
