package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactCard;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * A model class that represents a message holding a contact inside
 */
@ProtobufMessageName("Message.ContactMessage")
public record ContactMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String name,
        @ProtobufProperty(index = 16, type = ProtobufType.STRING)
        @NonNull
        ContactCard vcard,
        @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage {
    @Override
    public MessageType type() {
        return MessageType.CONTACT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}