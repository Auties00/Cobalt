package it.auties.whatsapp.model.message.business;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.BusinessMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.product.ProductCatalog;
import it.auties.whatsapp.model.product.ProductSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a message holding a product inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newProductMessage")
@Jacksonized
@Accessors(fluent = true)
public final class ProductMessage extends ContextualMessage implements BusinessMessage {
    /**
     * The product that this message wraps
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = ProductSnapshot.class)
    private ProductSnapshot product;

    /**
     * The jid of the WhatsappBusiness account that owns the product that this message wraps
     */
    @ProtobufProperty(index = 2, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
    private ContactJid businessOwnerId;

    /**
     * The catalog where the product that this message wraps is
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ProductCatalog.class)
    private ProductCatalog catalog;
}
