package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.product.ProductCatalog;
import it.auties.whatsapp.model.product.ProductSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a message holding a product inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newProductMessageBuilder")
@Jacksonized
@Accessors(fluent = true)
public final class ProductMessage extends ContextualMessage implements ButtonMessage {
    /**
     * The product that this message wraps
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = ProductSnapshot.class)
    private ProductSnapshot product;

    /**
     * The jid of the WhatsappBusiness account that owns the product that this message wraps
     */
    @ProtobufProperty(index = 2, type = STRING, implementation = ContactJid.class)
    private ContactJid businessOwnerJid;

    /**
     * The catalog where the product that this message wraps is
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = ProductCatalog.class)
    private ProductCatalog catalog;

    /**
     * The body of this message
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String body;

    /**
     * The footer of this message
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String footer;

    @Override
    public MessageType type() {
        return MessageType.PRODUCT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}
