package it.auties.whatsapp.model.info;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.product.ProductListHeaderImage;
import it.auties.whatsapp.model.product.ProductSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that holds the information related to a list of products.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ProductListInfo implements Info {
    /**
     * The products that this message wraps
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = ProductSection.class, repeated = true)
    private List<ProductSection> productSections;

    /**
     * The header image of the messages that this message wraps
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ProductListHeaderImage.class)
    private ProductListHeaderImage headerImage;

    /**
     * The jid of the seller of the products that this message wraps
     */
    @ProtobufProperty(index = 3, type = STRING, implementation = ContactJid.class)
    private ContactJid seller;
}
