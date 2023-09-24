package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a product
 */
@ProtobufMessageName("Message.ListMessage.Product")
public record ProductSectionEntry(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String id
) implements ProtobufMessage {

}
