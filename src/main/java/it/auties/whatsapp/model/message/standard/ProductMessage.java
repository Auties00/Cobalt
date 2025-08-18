package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.product.Product;
import it.auties.whatsapp.model.product.ProductCatalog;

import java.util.Optional;

/**
 * A model class that represents a message holding a product inside
 */
@ProtobufMessage(name = "Message.ProductMessage")
public final class ProductMessage implements ContextualMessage<ProductMessage>, ButtonMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final Product product;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final Jid businessOwnerJid;
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private final ProductCatalog catalog;
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private final String body;
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final String footer;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public ProductMessage(Product product, Jid businessOwnerJid, ProductCatalog catalog, String body, String footer, ContextInfo contextInfo) {
        this.product = product;
        this.businessOwnerJid = businessOwnerJid;
        this.catalog = catalog;
        this.body = body;
        this.footer = footer;
        this.contextInfo = contextInfo;
    }

    @Override
    public MessageType type() {
        return MessageType.PRODUCT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public Product product() {
        return product;
    }

    public Jid businessOwnerJid() {
        return businessOwnerJid;
    }

    public ProductCatalog catalog() {
        return catalog;
    }

    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public ProductMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "ProductMessage[" +
                "product=" + product + ", " +
                "businessOwnerJid=" + businessOwnerJid + ", " +
                "catalog=" + catalog + ", " +
                "body=" + body + ", " +
                "footer=" + footer + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}