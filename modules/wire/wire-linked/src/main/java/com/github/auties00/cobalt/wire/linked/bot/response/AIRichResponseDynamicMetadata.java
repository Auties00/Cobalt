package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Metadata for a dynamic media fragment within a WhatsApp AI bot rich
 * response, such as an animated GIF or a statically loaded image.
 *
 * <p>The media is fetched from the provided {@linkplain #url() URL}
 * and rendered according to its {@linkplain #type() type}. For GIF
 * content, the {@linkplain #loopCount() loop count} controls how
 * many times the animation plays before stopping.
 *
 * <p>This type implements {@link AIRichResponseSubMessageContent} and
 * appears as the {@link AIRichResponseSubMessageType#DYNAMIC DYNAMIC}
 * variant within an {@link AIRichResponseSubMessage}.
 *
 * @see AIRichResponseSubMessage#content()
 */
@ProtobufMessage(name = "AIRichResponseDynamicMetadata")
public final class AIRichResponseDynamicMetadata implements AIRichResponseSubMessageContent {
    /**
     * The media type of this dynamic content.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    AIRichResponseDynamicMediaType type;

    /**
     * A version identifier for this dynamic media asset, used for
     * cache invalidation.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long version;

    /**
     * The URL from which the dynamic media content is fetched.
     *
     * <p>Example: {@code "https://media.tenor.com/videos/abc123/mp4"}
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String url;

    /**
     * The number of times a GIF animation should loop before stopping.
     *
     * <p>A value of {@code 0} typically means the animation loops
     * indefinitely.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer loopCount;


    /**
     * Constructs a new dynamic metadata instance.
     *
     * @param type      the media type, or {@code null}
     * @param version   the cache version identifier, or {@code null}
     * @param url       the media URL, or {@code null}
     * @param loopCount the GIF loop count, or {@code null}
     */
    AIRichResponseDynamicMetadata(AIRichResponseDynamicMediaType type, Long version, String url, Integer loopCount) {
        this.type = type;
        this.version = version;
        this.url = url;
        this.loopCount = loopCount;
    }

    /**
     * Returns the media type of this dynamic content.
     *
     * @return an {@link Optional} containing the media type, or empty
     *         if not set
     */
    public Optional<AIRichResponseDynamicMediaType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the version identifier for this dynamic media asset.
     *
     * @return an {@link OptionalLong} containing the version, or empty
     *         if not set
     */
    public OptionalLong version() {
        return version == null ? OptionalLong.empty() : OptionalLong.of(version);
    }

    /**
     * Returns the URL from which this dynamic media is fetched.
     *
     * @return an {@link Optional} containing the URL, or empty if not
     *         set
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the number of times a GIF animation should loop.
     *
     * @return an {@link OptionalInt} containing the loop count, or
     *         empty if not set
     */
    public OptionalInt loopCount() {
        return loopCount == null ? OptionalInt.empty() : OptionalInt.of(loopCount);
    }

    /**
     * Sets the media type of this dynamic content.
     *
     * @param type the media type to set
     */
    public void setType(AIRichResponseDynamicMediaType type) {
        this.type = type;
    }

    /**
     * Sets the version identifier for this dynamic media asset.
     *
     * @param version the version to set
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Sets the URL from which this dynamic media is fetched.
     *
     * @param url the URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the number of times a GIF animation should loop.
     *
     * @param loopCount the loop count to set
     */
    public void setLoopCount(Integer loopCount) {
        this.loopCount = loopCount;
    }

    /**
     * Media type for dynamic content within an AI rich response.
     */
    @ProtobufEnum(name = "AIRichResponseDynamicMetadata.AIRichResponseDynamicMetadataType")
    public static enum AIRichResponseDynamicMediaType {
        /**
         * An unrecognised or unsupported dynamic media type.
         */
        UNKNOWN(0),

        /**
         * A static image loaded dynamically from a URL.
         */
        IMAGE(1),

        /**
         * An animated GIF image.
         */
        GIF(2);

        /**
         * Constructs a dynamic media type constant with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        AIRichResponseDynamicMediaType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index for this media type.
         */
        final int index;

        /**
         * Returns the protobuf index associated with this media type.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
