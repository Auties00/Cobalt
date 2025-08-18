package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.contact.ContactCard;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;

/**
 * A model class that represents a message holding a contact inside
 */
@ProtobufMessage(name = "Message.ContactMessage")
public final class ContactMessage implements ContextualMessage<ContactMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final String name;
    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    private final ContactCard vcard;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    public ContactMessage(String name, ContactCard vcard, ContextInfo contextInfo) {
        this.name = name;
        this.vcard = vcard;
        this.contextInfo = contextInfo;
    }

    public static ContactMessage of(String name, ContactCard vcard) {
        return new ContactMessage(name, vcard, null);
    }

    @Override
    public MessageType type() {
        return MessageType.CONTACT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
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
    public ContactMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "ContactMessage[" +
                "name=" + name + ", " +
                "vcard=" + vcard + ", " +
                "contextInfo=" + contextInfo + ']';
    }

}