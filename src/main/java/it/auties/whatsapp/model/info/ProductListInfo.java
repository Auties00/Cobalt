package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.product.ProductListHeaderImage;
import it.auties.whatsapp.model.product.ProductSection;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * A model class that holds the information related to a list of products.
 */
public record ProductListInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT, repeated = true)
        List<ProductSection> productSections,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        @NonNull
        ProductListHeaderImage headerImage,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        @NonNull
        ContactJid seller
) implements Info, ProtobufMessage {

}
