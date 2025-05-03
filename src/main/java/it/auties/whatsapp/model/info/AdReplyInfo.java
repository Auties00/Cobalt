package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that holds the information related to an companion reply.
 */
@ProtobufMessage(name = "ContextInfo.AdReplyInfo")
public final class AdReplyInfo implements Info {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String advertiserName;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final MediaType mediaType;

    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    final byte[] thumbnail;

    @ProtobufProperty(index = 17, type = ProtobufType.STRING)
    final String caption;

    AdReplyInfo(String advertiserName, MediaType mediaType, byte[] thumbnail, String caption) {
        this.advertiserName = Objects.requireNonNull(advertiserName, "advertiserName cannot be null");
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType cannot be null");
        this.thumbnail = thumbnail;
        this.caption = caption;
    }

    public String advertiserName() {
        return advertiserName;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AdReplyInfo that
                && Objects.equals(advertiserName, that.advertiserName)
                && Objects.equals(mediaType, that.mediaType)
                && Arrays.equals(thumbnail, that.thumbnail)
                && Objects.equals(caption, that.caption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(advertiserName, mediaType, Arrays.hashCode(thumbnail), caption);
    }

    @Override
    public String toString() {
        return "AdReplyInfo[" +
                "advertiserName=" + advertiserName +
                ", mediaType=" + mediaType +
                ", thumbnail=" + Arrays.toString(thumbnail) +
                ", caption=" + caption +
                ']';
    }

    /**
     * The constants of this enumerated type describe the various types of companion that a
     * {@link AdReplyInfo} can link to
     */
    @ProtobufEnum(name = "ContextInfo.AdReplyInfo.MediaType")
    public enum MediaType {
        /**
         * Unknown type
         */
        NONE(0),
        /**
         * Image type
         */
        IMAGE(1),
        /**
         * Video type
         */
        VIDEO(2);

        final int index;

        MediaType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}