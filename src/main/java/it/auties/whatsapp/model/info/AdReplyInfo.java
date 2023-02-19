package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that holds the information related to an companion reply.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class AdReplyInfo implements Info {
    /**
     * The name of the advertiser that served the original companion
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String advertiserName;

    /**
     * The type of original companion
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = AdReplyInfo.AdReplyInfoMediaType.class)
    private AdReplyInfoMediaType mediaType;

    /**
     * The thumbnail of the original companion encoded as jpeg in an array of bytes
     */
    @ProtobufProperty(index = 16, type = BYTES)
    private byte[] thumbnail;

    /**
     * The caption of the original companion
     */
    @ProtobufProperty(index = 17, type = STRING)
    private String caption;

    /**
     * The constants of this enumerated type describe the various types of companion that a
     * {@link AdReplyInfo} can link to
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("MediaType")
    public enum AdReplyInfoMediaType {
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
        
        @Getter
        private final int index;

        @JsonCreator
        public static AdReplyInfoMediaType of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}