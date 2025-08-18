package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
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
@ProtobufMessage(name = "Message.ContactsArrayMessage")
public final class ContactsMessage implements ContextualMessage<ContactsMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String name;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final List<ContactMessage> contacts;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;


    public ContactsMessage(String name, List<ContactMessage> contacts, ContextInfo contextInfo) {
        this.name = name;
        this.contacts = contacts;
        this.contextInfo = contextInfo;
    }

    @Override
    public MessageType type() {
        return MessageType.CONTACT_ARRAY;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public String name() {
        return name;
    }

    public List<ContactMessage> contacts() {
        return contacts;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public ContactsMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "ContactsMessage[" +
                "name=" + name + ", " +
                "contacts=" + contacts + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}