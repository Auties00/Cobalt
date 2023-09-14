package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;
import it.auties.whatsapp.model.message.button.InteractiveMessageContentType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents a business collection
 */
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
    public InteractiveMessageContentType contentType() {
        return InteractiveMessageContentType.COLLECTION;
    }
}
