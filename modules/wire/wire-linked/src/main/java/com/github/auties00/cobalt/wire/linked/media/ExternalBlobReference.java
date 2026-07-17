package com.github.auties00.cobalt.wire.linked.media;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a pointer to application state synchronization data that has been
 * uploaded to WhatsApp's media CDN rather than embedded inline.
 *
 * <p>Application state patches (chat settings, contact actions, starred messages,
 * and similar per-account state) are normally transmitted inline within a sync
 * patch. When a patch exceeds configured size or mutation count thresholds, its
 * payload is serialized, encrypted, and uploaded to the media CDN; the resulting
 * CDN coordinates (direct path, server handle, size, and hashes) are then carried
 * in the patch through an instance of this class.
 *
 * <p>Although this type is not a user-facing media attachment, it reuses the same
 * CDN infrastructure as message media and therefore implements {@link MediaProvider}
 * so that the upload and download pipelines can treat it uniformly. The
 * {@link #mediaPath()} method always returns {@link MediaPath#APP_STATE}, and the
 * {@link #mediaUrl()} method is unused because external blobs are always fetched
 * through the direct path.
 */
@ProtobufMessage(name = "ExternalBlobReference")
public final class ExternalBlobReference implements MediaProvider {
    /**
     * The symmetric key used to encrypt the blob before it was uploaded and
     * required to decrypt it after download.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * The CDN direct path at which the encrypted blob can be retrieved.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * The opaque server-issued handle that identifies this upload on the CDN.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String handle;

    /**
     * The size of the original plaintext blob in bytes.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * The SHA-256 digest of the original plaintext blob, used for integrity
     * verification after decryption.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    /**
     * The SHA-256 digest of the encrypted blob, used for integrity verification
     * before decryption.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * Constructs a new external blob reference with the given CDN coordinates.
     *
     * @param mediaKey             the symmetric encryption key
     * @param mediaDirectPath      the CDN direct path
     * @param handle               the server-issued handle
     * @param mediaSize            the plaintext blob size in bytes
     * @param mediaSha256          the SHA-256 of the plaintext blob
     * @param mediaEncryptedSha256 the SHA-256 of the encrypted blob
     */
    ExternalBlobReference(byte[] mediaKey, String mediaDirectPath, String handle, Long mediaSize, byte[] mediaSha256, byte[] mediaEncryptedSha256) {
        this.mediaKey = mediaKey;
        this.mediaDirectPath = mediaDirectPath;
        this.handle = handle;
        this.mediaSize = mediaSize;
        this.mediaSha256 = mediaSha256;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Returns the symmetric key used to encrypt and decrypt the blob.
     *
     * @return an {@link Optional} containing the media key, or empty if not set
     */
    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the CDN direct path at which the encrypted blob can be retrieved.
     *
     * <p>This is a convenience accessor that mirrors the protobuf field name
     * {@code directPath}.
     *
     * @return an {@link Optional} containing the direct path, or empty if not set
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the CDN direct path at which the encrypted blob can be retrieved.
     *
     * @return an {@link Optional} containing the direct path, or empty if not set
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the opaque server-issued handle that identifies this upload on
     * the CDN.
     *
     * @return an {@link Optional} containing the handle, or empty if not set
     */
    public Optional<String> handle() {
        return Optional.ofNullable(handle);
    }

    /**
     * Returns the size of the plaintext blob in bytes.
     *
     * <p>This is a convenience accessor that mirrors the protobuf field name
     * {@code fileSizeBytes}.
     *
     * @return an {@link OptionalLong} containing the size, or empty if not set
     */
    public OptionalLong fileSizeBytes() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the size of the plaintext blob in bytes.
     *
     * @return an {@link OptionalLong} containing the size, or empty if not set
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the SHA-256 digest of the plaintext blob.
     *
     * <p>This is a convenience accessor that mirrors the protobuf field name
     * {@code fileSha256}.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the SHA-256 digest of the plaintext blob.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted blob.
     *
     * <p>This is a convenience accessor that mirrors the protobuf field name
     * {@code fileEncSha256}.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted blob.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the CDN media URL.
     *
     * <p>External blob references are always retrieved through the direct path,
     * so this method always returns an empty {@link Optional}. It is provided
     * only to satisfy the {@link MediaProvider} contract.
     *
     * @return an empty {@link Optional}
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.empty();
    }

    /**
     * Returns the media path classification for this reference.
     *
     * <p>External blob references always target the application state CDN
     * namespace, so this method always returns {@link MediaPath#APP_STATE}.
     *
     * @return {@link MediaPath#APP_STATE}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.APP_STATE;
    }

    /**
     * Sets the symmetric encryption key.
     *
     * @param mediaKey the media key bytes
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Sets the CDN direct path.
     *
     * @param mediaDirectPath the direct path
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    /**
     * Sets the opaque server-issued handle.
     *
     * @param handle the server handle
     */
    public void setHandle(String handle) {
        this.handle = handle;
    }

    /**
     * Sets the plaintext blob size in bytes.
     *
     * @param mediaSize the blob size
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Sets the SHA-256 digest of the plaintext blob.
     *
     * @param mediaSha256 the plaintext hash bytes
     */
    @Override
    public void setMediaSha256(byte[] mediaSha256) {
        this.mediaSha256 = mediaSha256;
    }

    /**
     * Sets the SHA-256 digest of the encrypted blob.
     *
     * @param mediaEncryptedSha256 the encrypted hash bytes
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Ignored. External blob references do not carry a media URL.
     *
     * <p>This method exists only to satisfy the {@link MediaProvider} contract
     * and performs no action.
     *
     * @param mediaUrl ignored
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
    }

    /**
     * Ignored. External blob references do not carry a media key timestamp.
     *
     * <p>This method exists only to satisfy the {@link MediaProvider} contract
     * and performs no action.
     *
     * @param timestamp ignored
     */
    @Override
    public void setMediaKeyTimestamp(Instant timestamp) {
    }
}
