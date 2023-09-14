package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * A model class that represents a section inside a list of products
 */
public record ProductSection(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String title,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT, repeated = true)
        List<ProductSectionEntry> products
) implements ProtobufMessage {

}
