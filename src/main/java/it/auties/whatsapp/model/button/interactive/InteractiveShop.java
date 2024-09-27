package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;


/**
 * A model class that represents a shop
 */
@ProtobufMessage(name = "Message.InteractiveMessage.ShopMessage")
public record InteractiveShop(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        SurfaceType surfaceType,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int version
) implements InteractiveMessageContent {
    @Override
    public Type contentType() {
        return Type.SHOP;
    }

    /**
     * The constants of this enumerated type describe the various types of surfaces that a
     * {@link InteractiveShop} can have
     */
    @ProtobufEnum(name = "Message.InteractiveMessage.ShopMessage.Surface")
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

        final int index;

        SurfaceType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }
}