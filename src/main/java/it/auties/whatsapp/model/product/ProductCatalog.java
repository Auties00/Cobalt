package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.standard.ImageMessage;

/**
 * A model class that represents a product catalog
 */
@ProtobufMessageName("Message.ProductMessage.CatalogSnapshot")
public record ProductCatalog(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        ImageMessage catalogImage,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String title,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String description
) implements ProtobufMessage {

}