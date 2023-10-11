package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message holding a list of contacts inside
 */
@ProtobufMessageName("Message.ContactsArrayMessage")
public record ContactsMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT, repeated = true)
        List<ContactMessage> contacts,
        @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage {

    @Override
    public MessageType type() {
        return MessageType.CONTACT_ARRAY;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}