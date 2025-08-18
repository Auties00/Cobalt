package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactCard;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;

import java.util.Optional;

/**
 * A model class that represents a message holding a contact inside
 */
@ProtobufMessage(name = "Message.ContactMessage")
public final class ContactMessage implements ContextualMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    final ContactCard vcard;

    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    ContactMessage(String name, ContactCard vcard, ContextInfo contextInfo) {
        this.name = name;
        this.vcard = vcard;
        this.contextInfo = contextInfo;
    }

    @Override
    public Type type() {
        return Type.CONTACT;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }

    public String name() {
        return name;
    }

    public ContactCard vcard() {
        return vcard;
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
        return "ContactMessage[" +
                "name=" + name + ", " +
                "vcard=" + vcard + ", " +
                "contextInfo=" + contextInfo + ']';
    }
}