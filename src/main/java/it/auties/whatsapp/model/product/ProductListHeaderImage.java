package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents the header of a product list
 */
@ProtobufMessageName("Message.ListMessage.ProductListHeaderImage")
public record ProductListHeaderImage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte @NonNull [] thumbnail
) implements ProtobufMessage {

}