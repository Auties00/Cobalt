package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message holding a list of contacts inside
 */
@ProtobufMessage(name = "Message.ContactsArrayMessage")
public final class ContactsMessage implements ContextualMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<ContactMessage> contacts;

    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    ContactsMessage(String name, List<ContactMessage> contacts, ContextInfo contextInfo) {
        this.name = name;
        this.contacts = contacts;
        this.contextInfo = contextInfo;
    }

    @Override
    public Type type() {
        return Type.CONTACT_ARRAY;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
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
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    @Override
    public String toString() {
        return "ContactsMessage[" +
                "name=" + name + ", " +
                "contacts=" + contacts + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}