package it.auties.whatsapp.model.message.business;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.model.BusinessMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents a message holding a shop inside
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ShopMessage implements BusinessMessage {
    /**
     * The id of the shop
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    /**
     * The surface of the shop
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ShopMessageSurface.class)
    private ShopMessageSurface surface;

    /**
     * The version of the message
     */
    @ProtobufProperty(index = 3, type = INT32)
    private int messageVersion;

    /**
     * The constants of this enumerated type describe the various types of surfaces that a {@link ShopMessage} can have
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ShopMessageSurface {
        /**
         * Unknown
         */
        UNKNOWN_SURFACE(0),

        /**
         * Facebook
         */
        FB(1),

        /**
         * Instagram
         */
        IG(2),

        /**
         * Whatsapp
         */
        WA(3);

        @Getter
        private final int index;

        public static ShopMessageSurface forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
