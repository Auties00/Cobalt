package com.github.auties00.cobalt.wire.linked.media;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.*;

/**
 * A single transcoded rendition of a video message produced by the server.
 *
 * <p>When a video is uploaded to WhatsApp, the server may transcode it into
 * several versions targeting different resolutions, bitrates, and codec
 * profiles so that recipients can pick the rendition best suited to their
 * network conditions and device capabilities. Each of those renditions is
 * described by an instance of this class, carrying the CDN location of the
 * transcoded file, its size and dimensions, the encoding bitrate, a
 * {@link VideoQuality} tier, and a list of capability tokens that describe
 * playback prerequisites such as codec support.
 *
 * <p>The client typically inspects the full list of renditions attached to a
 * video message and chooses the best match based on the current network and
 * the capabilities the playback environment supports.
 */
@ProtobufMessage(name = "ProcessedVideo")
public final class ProcessedVideo {
    /**
     * The CDN direct path at which this rendition can be retrieved.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String directPath;

    /**
     * The SHA-256 digest of the rendition file, used for integrity verification.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] fileSha256;

    /**
     * The height of the rendition frame in pixels.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer height;

    /**
     * The width of the rendition frame in pixels.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer width;

    /**
     * The size of the rendition file in bytes.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    Long fileLength;

    /**
     * The encoding bitrate of the rendition in bits per second.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer bitrate;

    /**
     * The quality tier of this rendition.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    VideoQuality quality;

    /**
     * The list of capability tokens describing playback prerequisites such
     * as required codecs or features.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    List<String> capabilities;

    /**
     * Constructs a new {@code ProcessedVideo} with the given rendition metadata.
     *
     * @param directPath   the CDN direct path
     * @param fileSha256   the SHA-256 digest of the rendition file
     * @param height       the frame height in pixels
     * @param width        the frame width in pixels
     * @param fileLength   the file size in bytes
     * @param bitrate      the encoding bitrate in bits per second
     * @param quality      the quality tier
     * @param capabilities the playback capability tokens
     */
    ProcessedVideo(String directPath, byte[] fileSha256, Integer height, Integer width, Long fileLength, Integer bitrate, VideoQuality quality, List<String> capabilities) {
        this.directPath = directPath;
        this.fileSha256 = fileSha256;
        this.height = height;
        this.width = width;
        this.fileLength = fileLength;
        this.bitrate = bitrate;
        this.quality = quality;
        this.capabilities = capabilities;
    }

    /**
     * Returns the CDN direct path at which this rendition can be retrieved.
     *
     * @return an {@link Optional} containing the direct path, or empty if not set
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(directPath);
    }

    /**
     * Returns the SHA-256 digest of the rendition file.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(fileSha256);
    }

    /**
     * Returns the height of the rendition frame in pixels.
     *
     * @return an {@link OptionalInt} containing the height, or empty if not set
     */
    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    /**
     * Returns the width of the rendition frame in pixels.
     *
     * @return an {@link OptionalInt} containing the width, or empty if not set
     */
    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the size of the rendition file in bytes.
     *
     * @return an {@link OptionalLong} containing the file length, or empty if not set
     */
    public OptionalLong fileLength() {
        return fileLength == null ? OptionalLong.empty() : OptionalLong.of(fileLength);
    }

    /**
     * Returns the encoding bitrate of the rendition in bits per second.
     *
     * @return an {@link OptionalInt} containing the bitrate, or empty if not set
     */
    public OptionalInt bitrate() {
        return bitrate == null ? OptionalInt.empty() : OptionalInt.of(bitrate);
    }

    /**
     * Returns the quality tier of this rendition.
     *
     * @return an {@link Optional} containing the quality tier, or empty if not set
     */
    public Optional<VideoQuality> quality() {
        return Optional.ofNullable(quality);
    }

    /**
     * Returns the playback capability tokens required for this rendition.
     *
     * @return an unmodifiable list of capability tokens, or an empty list if none are set
     */
    public List<String> capabilities() {
        return capabilities == null ? List.of() : Collections.unmodifiableList(capabilities);
    }

    /**
     * Sets the CDN direct path for this rendition.
     *
     * @param directPath the direct path
     */
    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    /**
     * Sets the SHA-256 digest of the rendition file.
     *
     * @param fileSha256 the hash bytes
     */
    public void setFileSha256(byte[] fileSha256) {
        this.fileSha256 = fileSha256;
    }

    /**
     * Sets the frame height in pixels.
     *
     * @param height the height
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Sets the frame width in pixels.
     *
     * @param width the width
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Sets the file size in bytes.
     *
     * @param fileLength the file length
     */
    public void setFileLength(Long fileLength) {
        this.fileLength = fileLength;
    }

    /**
     * Sets the encoding bitrate in bits per second.
     *
     * @param bitrate the bitrate
     */
    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    /**
     * Sets the quality tier of this rendition.
     *
     * @param quality the quality tier
     */
    public void setQuality(VideoQuality quality) {
        this.quality = quality;
    }

    /**
     * Sets the playback capability tokens.
     *
     * @param capabilities the capability tokens
     */
    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * The quality tier of a processed video rendition.
     *
     * <p>Quality tiers are coarse-grained buckets that the client uses to
     * rank renditions without inspecting exact resolution or bitrate figures.
     * The client typically picks the highest tier supported by the current
     * network and device.
     */
    @ProtobufEnum(name = "ProcessedVideo.VideoQuality")
    public enum VideoQuality {
        /**
         * The quality tier is not specified.
         *
         * <p>Numeric value {@code 0}.
         */
        UNDEFINED(0),

        /**
         * Low quality targeting bandwidth-constrained connections.
         *
         * <p>Numeric value {@code 1}.
         */
        LOW(1),

        /**
         * Medium quality balancing visual fidelity and file size.
         *
         * <p>Numeric value {@code 2}.
         */
        MID(2),

        /**
         * High quality prioritizing resolution and bitrate over file size.
         *
         * <p>Numeric value {@code 3}.
         */
        HIGH(3);

        /**
         * Constructs a new {@code VideoQuality} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        VideoQuality(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index backing this quality tier.
         */
        final int index;

        /**
         * Returns the protobuf enum index of this quality tier.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }
}
