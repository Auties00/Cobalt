package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * One downloadable asset attached to a {@link NativeMachineLearningModel}.
 *
 * <p>Each asset carries the addressing fields the client uses to download and
 * cache it ({@code name}, {@code id}, {@code cacheKey}, {@code url}), the
 * integrity hashes the client verifies against ({@code sourceContentHash},
 * {@code md5Hash}), the server-issued opaque handle, the creation time marker,
 * the asset size in bytes, and the server-defined compression-type and
 * asset-type markers.
 *
 * <p>The creation-time, compression-type and asset-type markers are exposed as
 * raw strings because their wire format and value sets are not recoverable from
 * the WhatsApp client.
 */
@ProtobufMessage(name = "NativeMachineLearningModelAsset")
public final class NativeMachineLearningModelAsset {
    /**
     * Asset name. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Server-issued asset identifier. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    /**
     * Server-issued cache key the client uses to deduplicate downloads.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String cacheKey;

    /**
     * Source-content hash the client verifies the download against.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String sourceContentHash;

    /**
     * MD5 hash the client verifies the download against. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String md5Hash;

    /**
     * Server-issued opaque asset handle. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String assetHandle;

    /**
     * Server-defined creation-time marker. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String creationTime;

    /**
     * URL the asset is downloaded from. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String url;

    /**
     * Asset size in bytes. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64)
    final Long sizeBytes;

    /**
     * Server-defined compression-type marker. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String compressionType;

    /**
     * Server-defined asset-type marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    final String assetType;

    /**
     * Constructs a new {@code NativeMachineLearningModelAsset}. The reference
     * arguments may be {@code null} when the server omitted them.
     *
     * @param name              the asset name, or {@code null}
     * @param id                the asset identifier, or {@code null}
     * @param cacheKey          the asset cache key, or {@code null}
     * @param sourceContentHash the source-content hash, or {@code null}
     * @param md5Hash           the MD5 hash, or {@code null}
     * @param assetHandle       the opaque asset handle, or {@code null}
     * @param creationTime      the creation-time marker, or {@code null}
     * @param url               the asset download URL, or {@code null}
     * @param sizeBytes         the asset size in bytes, or {@code null}
     * @param compressionType   the compression-type marker, or {@code null}
     * @param assetType         the asset-type marker, or {@code null}
     */
    NativeMachineLearningModelAsset(String name,
                                    String id,
                                    String cacheKey,
                                    String sourceContentHash,
                                    String md5Hash,
                                    String assetHandle,
                                    String creationTime,
                                    String url,
                                    Long sizeBytes,
                                    String compressionType,
                                    String assetType) {
        this.name = name;
        this.id = id;
        this.cacheKey = cacheKey;
        this.sourceContentHash = sourceContentHash;
        this.md5Hash = md5Hash;
        this.assetHandle = assetHandle;
        this.creationTime = creationTime;
        this.url = url;
        this.sizeBytes = sizeBytes;
        this.compressionType = compressionType;
        this.assetType = assetType;
    }

    /**
     * Returns the asset name.
     *
     * @return the asset name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the server-issued asset identifier.
     *
     * @return the asset identifier, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the server-issued cache key.
     *
     * @return the cache key, or empty when the server omitted it
     */
    public Optional<String> cacheKey() {
        return Optional.ofNullable(cacheKey);
    }

    /**
     * Returns the source-content hash.
     *
     * @return the source-content hash, or empty when the server omitted it
     */
    public Optional<String> sourceContentHash() {
        return Optional.ofNullable(sourceContentHash);
    }

    /**
     * Returns the MD5 hash.
     *
     * @return the MD5 hash, or empty when the server omitted it
     */
    public Optional<String> md5Hash() {
        return Optional.ofNullable(md5Hash);
    }

    /**
     * Returns the opaque asset handle.
     *
     * @return the asset handle, or empty when the server omitted it
     */
    public Optional<String> assetHandle() {
        return Optional.ofNullable(assetHandle);
    }

    /**
     * Returns the creation-time marker.
     *
     * @return the creation-time marker, or empty when the server omitted it
     */
    public Optional<String> creationTime() {
        return Optional.ofNullable(creationTime);
    }

    /**
     * Returns the asset download URL.
     *
     * @return the download URL, or empty when the server omitted it
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the asset size in bytes.
     *
     * @return the size in bytes, or empty when the server omitted it
     */
    public OptionalLong sizeBytes() {
        return sizeBytes == null ? OptionalLong.empty() : OptionalLong.of(sizeBytes);
    }

    /**
     * Returns the compression-type marker.
     *
     * @return the compression-type marker, or empty when the server omitted it
     */
    public Optional<String> compressionType() {
        return Optional.ofNullable(compressionType);
    }

    /**
     * Returns the asset-type marker.
     *
     * @return the asset-type marker, or empty when the server omitted it
     */
    public Optional<String> assetType() {
        return Optional.ofNullable(assetType);
    }
}
