package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a business collection
 */
@ProtobufMessageName("Message.InteractiveMessage.CollectionMessage")
public record InteractiveCollection(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        ContactJid business,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String id,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int version
) implements InteractiveMessageContent {


    @Override
    public Type contentType() {
        return Type.COLLECTION;
    }
}
