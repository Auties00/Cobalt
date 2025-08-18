package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents a product
 */
@ProtobufMessage(name = "Message.ListMessage.Product")
public record ProductSectionEntry(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String id
) {

}
