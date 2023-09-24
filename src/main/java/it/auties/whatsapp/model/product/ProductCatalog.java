package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a product catalog
 */
@ProtobufMessageName("Message.ProductMessage.CatalogSnapshot")
public record ProductCatalog(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        ImageMessage catalogImage,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String title,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        @NonNull
        String description
) implements ProtobufMessage {

}