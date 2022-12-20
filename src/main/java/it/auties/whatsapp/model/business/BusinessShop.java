package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a shop
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class BusinessShop
        implements ProtobufMessage {
    /**
     * The id of the shop
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    /**
     * The surface of the shop
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = SurfaceType.class)
    private SurfaceType surfaceType;

    /**
     * The version of the message
     */
    @ProtobufProperty(index = 3, type = INT32)
    private int version;

    /**
     * The constants of this enumerated type describe the various types of surfaces that a {@link BusinessShop} can have
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum SurfaceType {
        /**
         * Unknown
         */
        UNKNOWN_SURFACE(0),

        /**
         * Facebook
         */
        FACEBOOK(1),

        /**
         * Instagram
         */
        INSTAGRAM(2),

        /**
         * Whatsapp
         */
        WHATSAPP(3);

        @Getter
        private final int index;

        public static SurfaceType of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
