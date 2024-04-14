package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;


/**
 * A model class that holds the information related to an companion reply.
 */
@ProtobufMessageName("ContextInfo.AdReplyInfo")
public record AdReplyInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String advertiserName,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        MediaType mediaType,
        @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
        Optional<byte[]> thumbnail,
        @ProtobufProperty(index = 17, type = ProtobufType.STRING)
        Optional<String> caption
) implements Info, ProtobufMessage {

    /**
     * The constants of this enumerated type describe the various types of companion that a
     * {@link AdReplyInfo} can link to
     */
    @ProtobufMessageName("ContextInfo.AdReplyInfo.MediaType")
    public enum MediaType implements ProtobufEnum {
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

        public int index() {
            return index;
        }
    }
}