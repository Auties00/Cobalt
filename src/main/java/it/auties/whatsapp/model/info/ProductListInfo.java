package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.product.ProductListHeaderImage;
import it.auties.whatsapp.model.product.ProductSection;

import java.util.List;

/**
 * A model class that holds the information related to a list of products.
 */
@ProtobufMessage(name = "Message.ListMessage.ProductListInfo")
public record ProductListInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<ProductSection> productSections,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        ProductListHeaderImage headerImage,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Jid seller
) implements Info {

}
