package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;
import it.auties.whatsapp.model.message.button.InteractiveMessageContentType;


/**
 * A model class that represents a shop
 */
public record InteractiveShop(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        InteractiveShopSurfaceType surfaceType,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int version
) implements InteractiveMessageContent {
    @Override
    public InteractiveMessageContentType contentType() {
        return InteractiveMessageContentType.SHOP;
    }
}