package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * A model class that represents a section inside a list of products
 */
@ProtobufMessageName("Message.ListMessage.ProductSection")
public record ProductSection(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        List<ProductSectionEntry> products
) implements ProtobufMessage {

}
