package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;


/**
 * A model class that holds the information related to an advertisement.
 */
@ProtobufMessage(name = "ContextInfo.ExternalAdReplyInfo")
public record ExternalAdReplyInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Optional<String> title,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Optional<String> body,
        @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
        Optional<MediaType> mediaType,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Optional<String> thumbnailUrl,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        Optional<String> mediaUrl,
        @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
        Optional<byte[]> thumbnail,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        Optional<String> sourceType,
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        Optional<String> sourceId,
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        Optional<String> sourceUrl,
        @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
        boolean containsAutoReply,
        @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
        boolean renderLargerThumbnail,
        @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
        boolean showAdAttribution,
        @ProtobufProperty(index = 13, type = ProtobufType.STRING)
        Optional<String> ctwaClid
) implements Info {
    /**
     * The constants of this enumerated type describe the various types of media that an ad can wrap
     */
    @ProtobufEnum(name = "ChatRowOpaqueData.DraftMessage.CtwaContextData.ContextInfoExternalAdReplyInfoMediaType")
    public enum MediaType {
        /**
         * No media
         */
        NONE(0),

        /**
         * Image
         */
        IMAGE(1),

        /**
         * Video
         */
        VIDEO(2);

        final int index;

        MediaType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}