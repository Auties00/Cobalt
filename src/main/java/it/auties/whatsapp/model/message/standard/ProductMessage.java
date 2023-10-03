package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.product.Product;
import it.auties.whatsapp.model.product.ProductCatalog;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A model class that represents a message holding a product inside
 */
@ProtobufMessageName("Message.ProductMessage")
public record ProductMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        Product product,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        Jid businessOwnerJid,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        @NonNull
        ProductCatalog catalog,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        Optional<String> body,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        Optional<String> footer,
        @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage, ButtonMessage {
    @Override
    public MessageType type() {
        return MessageType.PRODUCT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}