package com.github.auties00.cobalt.wire.linked.media;

import java.time.Instant;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Metadata describing a single sticker as synchronized across a user's
 * companion devices.
 *
 * <p>When a user adds a sticker to their collection on one device, the sticker
 * must be made available on every companion device that shares the account.
 * This type carries everything a companion needs to reconstruct the sticker:
 * the CDN location of the encrypted image ({@link #url()} and
 * {@link #directPath()}), the cryptographic material for decryption and
 * integrity checks ({@link #mediaKey()}, {@link #fileSha256()},
 * {@link #fileEncSha256()}), the visual dimensions, and flags describing the
 * sticker kind (animated Lottie, personalized avatar sticker).
 *
 * <p>The {@link #weight()} value and {@link #lastStickerSentTs()} timestamp are
 * used by the sticker picker UI to order stickers by recency and user affinity.
 */
@ProtobufMessage(name = "StickerMetadata")
public final class StickerMetadata {
    /**
     * The CDN URL at which the encrypted sticker can be downloaded.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String url;

    /**
     * The SHA-256 digest of the plaintext sticker file.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] fileSha256;

    /**
     * The SHA-256 digest of the encrypted sticker file.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] fileEncSha256;

    /**
     * The symmetric key used to decrypt the sticker file.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * The MIME type of the sticker file, typically {@code "image/webp"} for
     * both static and animated stickers.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String mimetype;

    /**
     * The height of the sticker image in pixels.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer height;

    /**
     * The width of the sticker image in pixels.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    Integer width;

    /**
     * The CDN direct path at which the encrypted sticker file can be fetched.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String directPath;

    /**
     * The size of the sticker file in bytes.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT64)
    Long fileLength;

    /**
     * A ranking weight used by the sticker picker for ordering stickers.
     * Higher values indicate stickers the user interacts with more often.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.FLOAT)
    Float weight;

    /**
     * The epoch-second timestamp of when the user last sent this sticker.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.INT64)
    Long lastStickerSentTs;

    /**
     * Whether this sticker is animated using the Lottie format rather than a
     * static WebP image.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    Boolean isLottie;

    /**
     * A perceptual hash of the sticker image used to identify duplicates and
     * near-duplicates.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String imageHash;

    /**
     * Whether this sticker is a personalized avatar sticker generated from
     * the user's avatar.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    Boolean isAvatarSticker;

    /**
     * Constructs a new {@code StickerMetadata} with the given fields.
     *
     * @param url               the CDN URL
     * @param fileSha256        the plaintext hash bytes
     * @param fileEncSha256     the encrypted hash bytes
     * @param mediaKey          the encryption key
     * @param mimetype          the MIME type
     * @param height            the image height in pixels
     * @param width             the image width in pixels
     * @param directPath        the CDN direct path
     * @param fileLength        the file size in bytes
     * @param weight            the ranking weight
     * @param lastStickerSentTs the epoch-second timestamp of last send
     * @param isLottie          whether the sticker uses Lottie animation
     * @param imageHash         the perceptual image hash
     * @param isAvatarSticker   whether this is an avatar sticker
     */
    StickerMetadata(String url, byte[] fileSha256, byte[] fileEncSha256, byte[] mediaKey, String mimetype, Integer height, Integer width, String directPath, Long fileLength, Float weight, Long lastStickerSentTs, Boolean isLottie, String imageHash, Boolean isAvatarSticker) {
        this.url = url;
        this.fileSha256 = fileSha256;
        this.fileEncSha256 = fileEncSha256;
        this.mediaKey = mediaKey;
        this.mimetype = mimetype;
        this.height = height;
        this.width = width;
        this.directPath = directPath;
        this.fileLength = fileLength;
        this.weight = weight;
        this.lastStickerSentTs = lastStickerSentTs;
        this.isLottie = isLottie;
        this.imageHash = imageHash;
        this.isAvatarSticker = isAvatarSticker;
    }

    /**
     * Returns the CDN URL at which the encrypted sticker can be downloaded.
     *
     * @return an {@link Optional} containing the URL, or empty if not set
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the SHA-256 digest of the plaintext sticker file.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(fileSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted sticker file.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(fileEncSha256);
    }

    /**
     * Returns the symmetric key used to decrypt the sticker file.
     *
     * @return an {@link Optional} containing the media key, or empty if not set
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the MIME type of the sticker file.
     *
     * @return an {@link Optional} containing the MIME type, or empty if not set
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the height of the sticker image in pixels.
     *
     * @return an {@link OptionalInt} containing the height, or empty if not set
     */
    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    /**
     * Returns the width of the sticker image in pixels.
     *
     * @return an {@link OptionalInt} containing the width, or empty if not set
     */
    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the CDN direct path at which the encrypted sticker file can be
     * fetched.
     *
     * @return an {@link Optional} containing the direct path, or empty if not set
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(directPath);
    }

    /**
     * Returns the size of the sticker file in bytes.
     *
     * @return an {@link OptionalLong} containing the file length, or empty if not set
     */
    public OptionalLong fileLength() {
        return fileLength == null ? OptionalLong.empty() : OptionalLong.of(fileLength);
    }

    /**
     * Returns the ranking weight used by the sticker picker.
     *
     * @return an {@link OptionalDouble} containing the weight, or empty if not set
     */
    public OptionalDouble weight() {
        return weight == null ? OptionalDouble.empty() : OptionalDouble.of(weight);
    }

    /**
     * Returns the timestamp of when the user last sent this sticker.
     *
     * @return an {@link Optional} containing the instant, or empty if not set
     */
    public Optional<Instant> lastStickerSentTs() {
        return lastStickerSentTs == null ? Optional.empty() : Optional.of(Instant.ofEpochSecond(lastStickerSentTs));
    }

    /**
     * Returns whether the sticker is animated using the Lottie format.
     *
     * @return {@code true} if the sticker is a Lottie animation, {@code false}
     *         otherwise or if not set
     */
    public boolean isLottie() {
        return isLottie != null && isLottie;
    }

    /**
     * Returns the perceptual hash of the sticker image.
     *
     * @return an {@link Optional} containing the image hash, or empty if not set
     */
    public Optional<String> imageHash() {
        return Optional.ofNullable(imageHash);
    }

    /**
     * Returns whether this sticker is a personalized avatar sticker.
     *
     * @return {@code true} if this is an avatar sticker, {@code false}
     *         otherwise or if not set
     */
    public boolean isAvatarSticker() {
        return isAvatarSticker != null && isAvatarSticker;
    }

    /**
     * Sets the CDN URL for the sticker file.
     *
     * @param url the CDN URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the SHA-256 digest of the plaintext sticker file.
     *
     * @param fileSha256 the plaintext hash bytes
     */
    public void setFileSha256(byte[] fileSha256) {
        this.fileSha256 = fileSha256;
    }

    /**
     * Sets the SHA-256 digest of the encrypted sticker file.
     *
     * @param fileEncSha256 the encrypted hash bytes
     */
    public void setFileEncSha256(byte[] fileEncSha256) {
        this.fileEncSha256 = fileEncSha256;
    }

    /**
     * Sets the symmetric key for decrypting the sticker file.
     *
     * @param mediaKey the encryption key
     */
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Sets the MIME type of the sticker file.
     *
     * @param mimetype the MIME type
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Sets the height of the sticker image in pixels.
     *
     * @param height the image height
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Sets the width of the sticker image in pixels.
     *
     * @param width the image width
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Sets the CDN direct path for the sticker file.
     *
     * @param directPath the direct path
     */
    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    /**
     * Sets the size of the sticker file in bytes.
     *
     * @param fileLength the file length
     */
    public void setFileLength(Long fileLength) {
        this.fileLength = fileLength;
    }

    /**
     * Sets the ranking weight used by the sticker picker.
     *
     * @param weight the ranking weight
     */
    public void setWeight(Float weight) {
        this.weight = weight;
    }

    /**
     * Sets the timestamp of when the user last sent this sticker.
     *
     * @param lastStickerSentTs the instant, or {@code null} to clear
     */
    public void setLastStickerSentTs(Instant lastStickerSentTs) {
        this.lastStickerSentTs = lastStickerSentTs == null ? null : lastStickerSentTs.getEpochSecond();
    }

    /**
     * Sets whether this sticker is animated using the Lottie format.
     *
     * @param isLottie {@code true} for Lottie animated stickers
     */
    public void setLottie(Boolean isLottie) {
        this.isLottie = isLottie;
    }

    /**
     * Sets the perceptual hash of the sticker image.
     *
     * @param imageHash the image hash
     */
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    /**
     * Sets whether this sticker is a personalized avatar sticker.
     *
     * @param isAvatarSticker {@code true} for avatar stickers
     */
    public void setAvatarSticker(Boolean isAvatarSticker) {
        this.isAvatarSticker = isAvatarSticker;
    }
}
