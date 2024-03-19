package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents the header of a product list
 */
@ProtobufMessageName("Message.ListMessage.ProductListHeaderImage")
public record ProductListHeaderImage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] thumbnail
) implements ProtobufMessage {

}